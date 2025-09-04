package sistemas_operacionais_I.trabalho_1_so_gupo_9;

import java.util.Random;

public class trabalho_1_so_grupo9 {

    // configuração básica
    static final int MAX_PROCS = 6, MAX_BURSTS = 10;
    static final int QUANTUM_HIGH = 4, QUANTUM_LOW = 2;       // quantum baixo = 2
    static final int CPU_MIN = 2, CPU_MAX = 4;                // CPU até 4
    static final int DISK_MIN = 1, DISK_MAX = 4;              // I/O por tipo
    static final int TAPE_MIN = 1, TAPE_MAX = 3;
    static final int PRINTER_MIN  = 2, PRINTER_MAX  = 4;
    static final int PROCS = 3;                               // quantos processos criar
    static final Random R = new Random();                     // sem seed fixa

    // ids de filas (2 de prontos + 3 de I/O)
    static final int Q_HIGH = 0, Q_LOW = 1, Q_DISK = 2, Q_TAPE = 3, Q_PRINTER = 4, QN = 5;

    // filas genéricas
    static int[][] queue = new int[QN][MAX_PROCS];
    static int[] head = new int[QN], tail = new int[QN], count = new int[QN];

    // estados e dispositivos
    static final int READY = 1, RUN = 2, BLOCK = 3, FIN = 4;
    static final int NONE = 0, DISK = 1, TAPE = 2, PRINTER = 3;

    // “PCB” em arrays simples
    static int[] pid = new int[MAX_PROCS];
    static int[] ppid = new int[MAX_PROCS];               // PPID (aqui sempre -1)
    static int[] priority = new int[MAX_PROCS];           // 1 = HIGH, 0 = LOW
    static int[] status = new int[MAX_PROCS];             // READY/RUN/BLOCK/FIN
    static int[] burstIndex = new int[MAX_PROCS];         // qual burst está
    static int[] cpuTimeLeft = new int[MAX_PROCS];        // tempo restante do burst de CPU atual
    static int[] ioTimeLeft = new int[MAX_PROCS];         // tempo restante de I/O
    static int[] quantumLeft = new int[MAX_PROCS];        // quantum restante
    static int[] cpuTotal = new int[MAX_PROCS];           // apenas estatística
    static int[] finishTime = new int[MAX_PROCS];         // turnaround (chegada = 0)
    static int[] waitReady = new int[MAX_PROCS];          // tempo esperando em prontos

    static int[][] cpuBurst = new int[MAX_PROCS][MAX_BURSTS];
    static int[][] ioDeviceType = new int[MAX_PROCS][MAX_BURSTS];
    static int[][] ioDuration = new int[MAX_PROCS][MAX_BURSTS];
    static int[] burstsCount = new int[MAX_PROCS];

    // controle “global”
    static int clock = 0, running = -1, runningCameFrom = -1, finished = 0;

    // util
    static int rnd(int a, int b) {
        return a + R.nextInt(b - a + 1);
    }

    // filas
    static boolean enqueue(int qid, int x) {
        if (count[qid] == MAX_PROCS) {
            return false;
        }
        queue[qid][tail[qid]] = x;
        tail[qid] = (tail[qid] + 1) % MAX_PROCS;
        count[qid]++;
        return true;
    }

    static int dequeue(int qid) {
        if (count[qid] == 0) {
            return -1;
        }
        int x = queue[qid][head[qid]];
        head[qid] = (head[qid] + 1) % MAX_PROCS;
        count[qid]--;
        return x;
    }

    // cria 1 processo aleatório (CPU 2..4 e I/O por tipo)
    static void makeProc(int i) {
        pid[i] = i;
        ppid[i] = -1; // sem fork: “sem pai”

        int bursts = rnd(1, MAX_BURSTS);
        burstsCount[i] = bursts;

        for (int k = 0; k < bursts; k++) {
            cpuBurst[i][k] = rnd(CPU_MIN, CPU_MAX);
            if (k < bursts - 1) {
                int device = rnd(1, 3); // 1 = DISK, 2 = TAPE, 3 = PRINTER
                ioDeviceType[i][k] = device;
                if (device == DISK) {
                    ioDuration[i][k] = rnd(DISK_MIN, DISK_MAX);
                } else if (device == TAPE) {
                    ioDuration[i][k] = rnd(TAPE_MIN, TAPE_MAX);
                } else {
                    ioDuration[i][k] = rnd(PRINTER_MIN, PRINTER_MAX);
                }
            } else {
                ioDeviceType[i][k] = NONE;
                ioDuration[i][k] = 0;
            }
        }

        status[i] = READY;
        burstIndex[i] = 0;
        cpuTimeLeft[i] = cpuBurst[i][0];
        quantumLeft[i] = QUANTUM_HIGH; // começa em HIGH
        priority[i] = 1;               // prioridade explícita
        enqueue(Q_HIGH, i);
    }

    // imprime workload (por extenso)
    static void dumpWork(int n) {
        System.out.println("== Carga de Trabalho ==");
        for (int i = 0; i < n; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("PID ").append(pid[i])
              .append(" (PPID = ").append(ppid[i])
              .append(", prioridade = ").append(priority[i] == 1 ? "HIGH" : "LOW")
              .append("): ");

            for (int k = 0; k < burstsCount[i]; k++) {
                sb.append("CPU ").append(cpuBurst[i][k]).append(" ");
                if (ioDeviceType[i][k] == DISK) {
                    sb.append("depois DISK ").append(ioDuration[i][k]).append(" ");
                } else if (ioDeviceType[i][k] == TAPE) {
                    sb.append("depois TAPE ").append(ioDuration[i][k]).append(" ");
                } else if (ioDeviceType[i][k] == PRINTER)  {
                    sb.append("depois PRINTER ").append(ioDuration[i][k]).append(" ");
                }
            }
            System.out.println(sb.toString().trim());
        }
    }

    // agenda (HIGH tem prioridade)
    static void schedule() {
        if (running != -1) {
            return;
        }

        int p = dequeue(Q_HIGH);
        if (p != -1) {
            running = p;
            runningCameFrom = 1;
            status[p] = RUN;
            quantumLeft[p] = QUANTUM_HIGH;
            System.out.println("t = " + clock + " | Escalonador: selecionou PID " + p + " da fila HIGH");
            return;
        }

        p = dequeue(Q_LOW);
        if (p != -1) {
            running = p;
            runningCameFrom = 0;
            status[p] = RUN;
            quantumLeft[p] = QUANTUM_LOW;
            System.out.println("t = " + clock + " | Escalonador: selecionou PID " + p + " da fila LOW");
        }
    }

    // I/O (processa só a cabeça de cada fila)
    static void stepIO() {
        if (count[Q_DISK] > 0) {
            int p = queue[Q_DISK][head[Q_DISK]];
            ioTimeLeft[p]--;
            if (ioTimeLeft[p] <= 0) {
                dequeue(Q_DISK);
                status[p] = READY;
                priority[p] = 0;
                enqueue(Q_LOW, p); // disco retorna para LOW
                System.out.println("t = " + clock + " | I/O concluído em DISK para PID " + p + " (retorna para LOW)");
            }
        }

        if (count[Q_TAPE] > 0) {
            int p = queue[Q_TAPE][head[Q_TAPE]];
            ioTimeLeft[p]--;
            if (ioTimeLeft[p] <= 0) {
                dequeue(Q_TAPE);
                status[p] = READY;
                priority[p] = 1;
                enqueue(Q_HIGH, p); // fita retorna para HIGH
                System.out.println("t = " + clock + " | I/O concluído em TAPE para PID " + p + " (retorna para HIGH)");
            }
        }

        if (count[Q_PRINTER] > 0) {
            int p = queue[Q_PRINTER][head[Q_PRINTER]];
            ioTimeLeft[p]--;
            if (ioTimeLeft[p] <= 0) {
                dequeue(Q_PRINTER);
                status[p] = READY;
                priority[p] = 1;
                enqueue(Q_HIGH, p); // impressora retorna para HIGH
                System.out.println("t = " + clock + " | I/O concluído em PRINTER para PID " + p + " (retorna para HIGH)");
            }
        }
    }

    // 1 tick de CPU (preempção, fim de burst, timeout)
    static void stepCPU() {
        // preempção: se LOW está rodando e existe alguém em HIGH, tira LOW
        if (running != -1 && runningCameFrom == 0 && count[Q_HIGH] > 0) {
            int p = running;
            status[p] = READY;
            priority[p] = 0;
            enqueue(Q_LOW, p);
            running = -1;
            System.out.println("t = " + clock + " | Preempção: removeu PID " + p + " (LOW) porque há processo em HIGH");
            return;
        }

        if (running == -1) {
            return;
        }

        int p = running;
        cpuTimeLeft[p]--;
        quantumLeft[p]--;
        cpuTotal[p]++;

        System.out.println("t = " + clock + " | Executando PID " + p + " (tempo de CPU restante = " + cpuTimeLeft[p] + ", quantum restante = " + quantumLeft[p] + ")");

        // fim do burst de CPU
        if (cpuTimeLeft[p] <= 0) {
            int k = burstIndex[p];
            int device = ioDeviceType[p][k];

            if (device == NONE) {
                status[p] = FIN;
                finished++;
                running = -1;
                finishTime[p] = clock; // se quiser contar o tick atual completo, use clock + 1
                System.out.println("t = " + clock + " | Finalização: PID " + p + " terminou a execução");
                return;
            }

            // vai para I/O do burst atual
            status[p] = BLOCK;
            ioTimeLeft[p] = ioDuration[p][k];
            burstIndex[p] = k + 1;
            if (burstIndex[p] < burstsCount[p]) {
                cpuTimeLeft[p] = cpuBurst[p][burstIndex[p]];
            }

            if (device == DISK) {
                enqueue(Q_DISK, p);
                System.out.println("t = " + clock + " | Início de I/O em DISK para PID " + p + " (duração = " + ioTimeLeft[p] + ")");
            } else if (device == TAPE) {
                enqueue(Q_TAPE, p);
                System.out.println("t = " + clock + " | Início de I/O em TAPE para PID " + p + " (duração = " + ioTimeLeft[p] + ")");
            } else {
                enqueue(Q_PRINTER, p);
                System.out.println("t = " + clock + " | Início de I/O em PRINTER para PID " + p + " (duração = " + ioTimeLeft[p] + ")");
            }

            running = -1;
            return;
        }

        // estourou o quantum (volta para LOW)
        if (quantumLeft[p] <= 0) {
            status[p] = READY;
            priority[p] = 0;
            enqueue(Q_LOW, p);
            running = -1;
            System.out.println("t = " + clock + " | Fim de quantum: PID " + p + " retorna para LOW (tempo de CPU restante = " + cpuTimeLeft[p] + ")");
        }
    }

    // acumula espera em prontos
    static void tickWaitReady() {
        for (int i = 0; i < PROCS; i++) {
            if (status[i] == READY) {
                waitReady[i]++;
            }
        }
    }

    // estatísticas simples
    static void stats() {
        System.out.println("== Estatísticas ==");
        double sumTurnaround = 0.0, sumWaitReady = 0.0;

        for (int i = 0; i < PROCS; i++) {
            sumTurnaround += finishTime[i];
            sumWaitReady += waitReady[i];
            System.out.println(
                "PID " + pid[i] +
                " (PPID = " + ppid[i] +
                ", prioridade final = " + (priority[i] == 1 ? "HIGH" : "LOW") + ")" +
                " | turnaround = " + finishTime[i] +
                " | espera em prontos = " + waitReady[i] +
                " | total de tempo de CPU = " + cpuTotal[i] +
                " | quantidade de bursts = " + burstsCount[i]
            );
        }

        System.out.println("média de turnaround = " + (sumTurnaround / PROCS));
        System.out.println("média de espera em prontos = " + (sumWaitReady / PROCS));
    }

    // main
    public static void main(String[] args) {

        // cria processos e mostra a carga de trabalho
        for (int i = 0; i < PROCS; i++) {
            makeProc(i);
        }
        dumpWork(PROCS);

        int MAX_TIME = 500;
        while (finished < PROCS && clock < MAX_TIME) {
            if (running == -1) {
                schedule();
            }
            stepIO();
            stepCPU();
            tickWaitReady();
            clock++;
        }

        System.out.println("--- Fim da simulação --- tempo = " + clock + " | processos finalizados = " + finished + " de " + PROCS);
        stats();
    }
}
