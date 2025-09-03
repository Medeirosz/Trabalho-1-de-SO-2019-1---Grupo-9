package sistemas_operacionais_I.trabalho_1_so_gupo_9;

import java.util.Random;

public class trabalho_1_so_grupo9 {

    // limites e quantuns
    static int MAX_PROCS = 6;
    static int MAX_BURSTS = 10;
    static int QUANTUM_HIGH = 4;
    static int QUANTUM_LOW  = 3;

    // geração aleatória
    static final int CPU_MIN = 2, CPU_MAX = 4;
    static final int IO_MIN = 1, IO_MAX = 2;
    static final int BURSTS_MIN = 1, BURSTS_MAX = 10;
    static final Random R = new Random();

    // índices de filas
    static final int QUEUE_HIGH = 0, QUEUE_LOW = 1, QUEUE_DISK = 2, QUEUE_TAPE = 3, QUEUE_PRINTER = 4;

    // cenário
    static int number_of_processes = 3;

    // controle da simulação
    static int clock_ticks = 0;
    static int running_process_id = -1;   // processo atual na CPU (-1 = livre)
    static int running_level = -1;        // 1 se veio da HIGH, 0 se veio da LOW
    static int finished_count = 0;

    // filas de prontos e I/O
    static int[] ready_queue_high = new int[MAX_PROCS];
    static int[] ready_queue_low  = new int[MAX_PROCS];
    static int[] io_queue_disk    = new int[MAX_PROCS];
    static int[] io_queue_tape    = new int[MAX_PROCS];
    static int[] io_queue_printer = new int[MAX_PROCS];

    // controle de fila circular (head/tail/count)
    static int ready_high_head,  ready_high_tail,  ready_high_count  = 0;
    static int ready_low_head,   ready_low_tail,   ready_low_count   = 0;
    static int disk_head,        disk_tail,        disk_count        = 0;
    static int tape_head,        tape_tail,        tape_count        = 0;
    static int printer_head,     printer_tail,     printer_count     = 0;

    // estados e dispositivos
    static final int STATE_NEW = 0, STATE_READY = 1, STATE_RUNNING = 2, STATE_BLOCKED = 3, STATE_FINISHED = 4;
    static final int IO_DEVICE_NONE = 0, IO_DEVICE_DISK = 1, IO_DEVICE_TAPE = 2, IO_DEVICE_PRINTER = 3;

    // “PCB” em arrays (índice = process_id)
    static int[] process_id            = new int[MAX_PROCS];
    static int[] parent_process_id     = new int[MAX_PROCS];
    static int[] process_status        = new int[MAX_PROCS];
    static int[] process_priority      = new int[MAX_PROCS]; // 1=alta, 0=baixa (preferência)
    static int[] current_burst_index   = new int[MAX_PROCS]; // índice do burst atual
    static int[] remaining_cpu_time    = new int[MAX_PROCS];
    static int[] remaining_io_time     = new int[MAX_PROCS];
    static int[] quantum_left          = new int[MAX_PROCS];
    static int[] total_cpu_time        = new int[MAX_PROCS];
    static int[] number_of_bursts      = new int[MAX_PROCS];

    static int[][] cpu_burst           = new int[MAX_PROCS][MAX_BURSTS];
    static int[][] io_type             = new int[MAX_PROCS][MAX_BURSTS];
    static int[][] io_duration         = new int[MAX_PROCS][MAX_BURSTS];

    // métricas simples
    static int[] finish_time           = new int[MAX_PROCS]; // turnaround (chegada = 0)
    static int[] ready_wait_time       = new int[MAX_PROCS]; // espera em filas de prontos

    // ------------------------------------------------------------
    // FILAS (enqueue / dequeue / peek)
    // ------------------------------------------------------------
    static boolean enqueue(int queue_id, int pid) {
        switch (queue_id) {
            case QUEUE_HIGH: {
                if (ready_high_count == MAX_PROCS) { return false; }
                ready_queue_high[ready_high_tail] = pid;
                ready_high_tail = (ready_high_tail + 1) % MAX_PROCS;
                ready_high_count++;
                return true;
            }
            case QUEUE_LOW: {
                if (ready_low_count == MAX_PROCS) { return false; }
                ready_queue_low[ready_low_tail] = pid;
                ready_low_tail = (ready_low_tail + 1) % MAX_PROCS;
                ready_low_count++;
                return true;
            }
            case QUEUE_DISK: {
                if (disk_count == MAX_PROCS) { return false; }
                io_queue_disk[disk_tail] = pid;
                disk_tail = (disk_tail + 1) % MAX_PROCS;
                disk_count++;
                return true;
            }
            case QUEUE_TAPE: {
                if (tape_count == MAX_PROCS) { return false; }
                io_queue_tape[tape_tail] = pid;
                tape_tail = (tape_tail + 1) % MAX_PROCS;
                tape_count++;
                return true;
            }
            case QUEUE_PRINTER: {
                if (printer_count == MAX_PROCS) { return false; }
                io_queue_printer[printer_tail] = pid;
                printer_tail = (printer_tail + 1) % MAX_PROCS;
                printer_count++;
                return true;
            }
        }
        return false;
    }

    static int dequeue(int queue_id) {
        switch (queue_id) {
            case QUEUE_HIGH: {
                if (ready_high_count == 0) { return -1; }
                int pid = ready_queue_high[ready_high_head];
                ready_high_head = (ready_high_head + 1) % MAX_PROCS;
                ready_high_count--;
                return pid;
            }
            case QUEUE_LOW: {
                if (ready_low_count == 0) { return -1; }
                int pid = ready_queue_low[ready_low_head];
                ready_low_head = (ready_low_head + 1) % MAX_PROCS;
                ready_low_count--;
                return pid;
            }
            case QUEUE_DISK: {
                if (disk_count == 0) { return -1; }
                int pid = io_queue_disk[disk_head];
                disk_head = (disk_head + 1) % MAX_PROCS;
                disk_count--;
                return pid;
            }
            case QUEUE_TAPE: {
                if (tape_count == 0) { return -1; }
                int pid = io_queue_tape[tape_head];
                tape_head = (tape_head + 1) % MAX_PROCS;
                tape_count--;
                return pid;
            }
            case QUEUE_PRINTER: {
                if (printer_count == 0) { return -1; }
                int pid = io_queue_printer[printer_head];
                printer_head = (printer_head + 1) % MAX_PROCS;
                printer_count--;
                return pid;
            }
        }
        return -1;
    }

    static int peek(int queue_id) {
        switch (queue_id) {
            case QUEUE_HIGH:   { return ready_high_count == 0 ? -1 : ready_queue_high[ready_high_head]; }
            case QUEUE_LOW:    { return ready_low_count  == 0 ? -1 : ready_queue_low[ready_low_head]; }
            case QUEUE_DISK:   { return disk_count       == 0 ? -1 : io_queue_disk[disk_head]; }
            case QUEUE_TAPE:   { return tape_count       == 0 ? -1 : io_queue_tape[tape_head]; }
            case QUEUE_PRINTER:{ return printer_count    == 0 ? -1 : io_queue_printer[printer_head]; }
        }
        return -1;
    }

    // ------------------------------------------------------------
    // WORKLOAD
    // ------------------------------------------------------------
    static void generate_process(int i) {
        process_id[i]        = i;
        parent_process_id[i] = 0; // "init" simples (atende o PDF)

        int bursts = BURSTS_MIN + R.nextInt(BURSTS_MAX - BURSTS_MIN + 1);
        bursts = Math.min(bursts, MAX_BURSTS);
        number_of_bursts[i] = bursts;

        for (int k = 0; k < bursts; k++) {
            cpu_burst[i][k] = CPU_MIN + R.nextInt(CPU_MAX - CPU_MIN + 1);
            if (k < bursts - 1) {
                int device = 1 + R.nextInt(3); // 1=DISK, 2=TAPE, 3=PRINTER
                io_type[i][k]     = device;
                io_duration[i][k] = IO_MIN + R.nextInt(IO_MAX - IO_MIN + 1);
            } else {
                io_type[i][k]     = IO_DEVICE_NONE;
                io_duration[i][k] = 0;
            }
        }

        process_status[i]      = STATE_READY;
        process_priority[i]    = 1; // inicia na alta
        current_burst_index[i] = 0;
        remaining_cpu_time[i]  = cpu_burst[i][0];
        remaining_io_time[i]   = 0;
        quantum_left[i]        = QUANTUM_HIGH;
        total_cpu_time[i]      = 0;

        enqueue(QUEUE_HIGH, i);
    }

    static void setup_random_workload(int n) {
        for (int i = 0; i < n; i++) {
            generate_process(i);
        }
        dump_workload(n);
    }

    static void dump_workload(int n) {
        System.out.println("== Workload (random) ==");
        for (int i = 0; i < n; i++) {
            StringBuilder sb = new StringBuilder("PID " + process_id[i] + " (PPID " + parent_process_id[i] + ") : ");
            for (int k = 0; k < number_of_bursts[i]; k++) {
                sb.append("CPU").append(cpu_burst[i][k]);
                if (io_type[i][k] == IO_DEVICE_DISK) {
                    sb.append("->DISK").append(io_duration[i][k]).append("->");
                } else if (io_type[i][k] == IO_DEVICE_TAPE) {
                    sb.append("->TAPE").append(io_duration[i][k]).append("->");
                } else if (io_type[i][k] == IO_DEVICE_PRINTER) {
                    sb.append("->PRINTER").append(io_duration[i][k]).append("->");
                }
            }
            if (sb.toString().endsWith("->")) {
                sb.setLength(sb.length() - 2);
            }
            System.out.println(sb);
        }
    }

    // ------------------------------------------------------------
    // ESCALONADOR / I/O / CPU
    // ------------------------------------------------------------
    static void schedule_process() {
        if (running_process_id != -1) { return; }

        int pid = dequeue(QUEUE_HIGH);
        if (pid != -1) {
            running_process_id = pid;
            running_level = 1;
            process_status[pid] = STATE_RUNNING;
            quantum_left[pid] = QUANTUM_HIGH;
            System.out.println("t = " + clock_ticks + " | SCHEDULE HIGH -> process_id = " + pid);
            return;
        }

        pid = dequeue(QUEUE_LOW);
        if (pid != -1) {
            running_process_id = pid;
            running_level = 0;
            process_status[pid] = STATE_RUNNING;
            quantum_left[pid] = QUANTUM_LOW;
            System.out.println("t = " + clock_ticks + " | SCHEDULE LOW  -> process_id = " + pid);
        }
    }

    static void step_io_tick() {
        if (disk_count > 0) {
            int pid = io_queue_disk[disk_head];
            remaining_io_time[pid]--;
            if (remaining_io_time[pid] <= 0) {
                dequeue(QUEUE_DISK);
                process_status[pid] = STATE_READY;
                process_priority[pid] = 0; // DISK volta baixa
                enqueue(QUEUE_LOW, pid);
                System.out.println("t = " + clock_ticks + " | IO_END DISK    process_id = " + pid + " -> LOW");
            }
        }

        if (tape_count > 0) {
            int pid = io_queue_tape[tape_head];
            remaining_io_time[pid]--;
            if (remaining_io_time[pid] <= 0) {
                dequeue(QUEUE_TAPE);
                process_status[pid] = STATE_READY;
                process_priority[pid] = 1; // TAPE volta alta
                enqueue(QUEUE_HIGH, pid);
                System.out.println("t = " + clock_ticks + " | IO_END TAPE    process_id = " + pid + " -> HIGH");
            }
        }

        if (printer_count > 0) {
            int pid = io_queue_printer[printer_head];
            remaining_io_time[pid]--;
            if (remaining_io_time[pid] <= 0) {
                dequeue(QUEUE_PRINTER);
                process_status[pid] = STATE_READY;
                process_priority[pid] = 1; // PRINTER volta alta
                enqueue(QUEUE_HIGH, pid);
                System.out.println("t = " + clock_ticks + " | IO_END PRINTER process_id = " + pid + " -> HIGH");
            }
        }
    }

    static void run_cpu_tick() {
        // preempção: LOW rodando e chega alguém na HIGH
        if (running_process_id != -1 && running_level == 0 && ready_high_count > 0) {
            int pid = running_process_id;
            process_status[pid] = STATE_READY;
            enqueue(QUEUE_LOW, pid);
            running_process_id = -1;
            System.out.println("t = " + clock_ticks + " | PREEMPT process_id = " + pid + " (LOW)");
            return;
        }

        if (running_process_id == -1) { return; }

        int pid = running_process_id;
        remaining_cpu_time[pid]--;
        quantum_left[pid]--;
        total_cpu_time[pid]++;

        System.out.println(
            "t = " + clock_ticks + " | RUN process_id = " + pid +
            " (cpu_remaining = " + remaining_cpu_time[pid] +
            ", quantum_left = " + quantum_left[pid] + ")"
        );

        // fim de CPU burst
        if (remaining_cpu_time[pid] <= 0) {
            int k = current_burst_index[pid];
            int device = io_type[pid][k];

            if (device == IO_DEVICE_NONE) {
                process_status[pid] = STATE_FINISHED;
                finished_count++;
                running_process_id = -1;
                finish_time[pid] = clock_ticks;
                System.out.println("t = " + clock_ticks + " | FINISH process_id = " + pid);
                return;
            }

            // vai para I/O do burst k
            process_status[pid] = STATE_BLOCKED;
            remaining_io_time[pid] = io_duration[pid][k];

            // prepara próximo CPU (se existir)
            current_burst_index[pid] = k + 1;
            if (current_burst_index[pid] < number_of_bursts[pid]) {
                remaining_cpu_time[pid] = cpu_burst[pid][current_burst_index[pid]];
            }

            if (device == IO_DEVICE_DISK) {
                enqueue(QUEUE_DISK, pid);
                System.out.println(
                    "t = " + clock_ticks + " | IO_START DISK    process_id = " + pid +
                    " | duration = " + remaining_io_time[pid]
                );
            } else if (device == IO_DEVICE_TAPE) {
                enqueue(QUEUE_TAPE, pid);
                System.out.println(
                    "t = " + clock_ticks + " | IO_START TAPE    process_id = " + pid +
                    " | duration = " + remaining_io_time[pid]
                );
            } else {
                enqueue(QUEUE_PRINTER, pid);
                System.out.println(
                    "t = " + clock_ticks + " | IO_START PRINTER process_id = " + pid +
                    " | duration = " + remaining_io_time[pid]
                );
            }

            running_process_id = -1;
            return;
        }

        // acabou o quantum e ainda tem CPU
        if (quantum_left[pid] <= 0) {
            process_status[pid] = STATE_READY;
            enqueue(QUEUE_LOW, pid); // vai pra LOW
            running_process_id = -1;
            System.out.println(
                "t = " + clock_ticks + " | TIMEOUT process_id = " + pid +
                " -> LOW (cpu_remaining = " + remaining_cpu_time[pid] + ")"
            );
        }
    }

    // MÉTRICAS / LOG AUXILIAR
    static void accrue_waiting_ready() {
        for (int i = 0; i < number_of_processes; i++) {
            if (process_status[i] == STATE_READY) {
                ready_wait_time[i]++;
            }
        }
    }

    static void print_statistics() {
        System.out.println("== Estatísticas ==");
        double sum_turnaround = 0, sum_wait = 0;
        for (int i = 0; i < number_of_processes; i++) {
            int turnaround = finish_time[i];
            sum_turnaround += turnaround;
            sum_wait += ready_wait_time[i];
            System.out.println("PID " + process_id[i] + " (PPID " + parent_process_id[i] + ")" +
                    " | turnaround = " + turnaround +
                    " | esperaReady = " + ready_wait_time[i] +
                    " | cpuTotal = " + total_cpu_time[i]);
        }
        System.out.println("média turnaround = " + (sum_turnaround / number_of_processes));
        System.out.println("média esperaReady = " + (sum_wait / number_of_processes));
    }

    // ------------------------------------------------------------
    // MAIN
    // ------------------------------------------------------------
    public static void main(String[] args) {
        setup_random_workload(number_of_processes);

        int MAX_TIME = 500;
        while (finished_count < number_of_processes && clock_ticks < MAX_TIME) {
            if (running_process_id == -1) { schedule_process(); }
            step_io_tick();
            run_cpu_tick();
            accrue_waiting_ready();
            clock_ticks++;
        }
        System.out.println("--- fim --- t = " + clock_ticks +
                " finished = " + finished_count + "/" + number_of_processes);
        print_statistics();
    }
}
