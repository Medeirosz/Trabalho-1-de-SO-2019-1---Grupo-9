package sistemas_operacionais_I.trabalho_1_so_gupo_9;

import java.util.Random;

public class trabalho_1_so_grupo9 {

    // limites e quantuns
    static int MAX_PROCS = 6;
    static int MAX_BURSTS = 10;
    static int QUANTUM_HIGH = 4;
    static int QUANTUM_LOW  = 1;

    // geração aleatória (mantém resultados repetíveis)
    static final int CPU_MIN=2, CPU_MAX=6;
    static final int IO_MIN =1, IO_MAX =4;
    static final int BURSTS_MIN=1, BURSTS_MAX=3;
    static final long SEED = 42L;
    static final Random R = new Random(SEED);

    // cenário
    static int NumberTest = 3;

    // controle da simulação
    static int ticks = 0;
    static int running = -1;  // pid atual na CPU (-1 = livre)
    static int level   = -1;  // 1 se veio da HIGH, 0 se da LOW
    static int finished = 0;

    // filas: 0=HIGH, 1=LOW, 2=DISK, 3=TAPE, 4=PRINTER
    static int[] highPriority = new int[MAX_PROCS];
    static int[] lowPriority  = new int[MAX_PROCS];
    static int[] ioDisk       = new int[MAX_PROCS];
    static int[] ioTape       = new int[MAX_PROCS];
    static int[] ioPrinter    = new int[MAX_PROCS];

    // controle de fila circular (head/tail/count)
    static int highPriorityHead, highPriorityTail, highPriorityCount = 0;
    static int lowPriorityHead,  lowPriorityTail,  lowPriorityCount  = 0;
    static int diskIoHead,       diskIoTail,       diskIoCount       = 0;
    static int tapeIoHead,       tapeIoTail,       tapeIoCount       = 0;
    static int printerIoHead,    printerIoTail,    printerIoCount    = 0;

    // estados e dispositivos
    static final int NEW=0, READY=1, RUNNING=2, BLOCKED=3, FINISHED=4;
    static final int IO_NONE=0, IO_DISK=1, IO_TAPE=2, IO_PRINTER=3;

    // “PCB” em arrays (índice = pid)
    static int[] pid         = new int[MAX_PROCS]; // NOVO: PID explícito
    static int[] ppid        = new int[MAX_PROCS]; // NOVO: PPID (usaremos 0=init)
    static int[] status      = new int[MAX_PROCS];
    static int[] priority    = new int[MAX_PROCS]; // 1=alta, 0=baixa (preferência)
    static int[] cur         = new int[MAX_PROCS]; // índice do burst atual
    static int[] remainCpu   = new int[MAX_PROCS];
    static int[] remainIo    = new int[MAX_PROCS];
    static int[] quantumLeft = new int[MAX_PROCS];
    static int[] totalCpu    = new int[MAX_PROCS];
    static int[] nb          = new int[MAX_PROCS]; // qtde de CPU bursts

    static int[][] cpuBurst  = new int[MAX_PROCS][MAX_BURSTS];
    static int[][] ioType    = new int[MAX_PROCS][MAX_BURSTS];
    static int[][] ioDur     = new int[MAX_PROCS][MAX_BURSTS];

    // métricas simples
    static int[] tFinish   = new int[MAX_PROCS]; // turnaround (chegada = 0)
    static int[] waitReady = new int[MAX_PROCS]; // espera em filas de prontos

    // filas (push/pop/peek)
    static boolean push(int fila, int pid) {
        switch (fila) {
            case 0:
                if (highPriorityCount == MAX_PROCS) return false;
                highPriority[highPriorityTail] = pid;
                highPriorityTail = (highPriorityTail + 1) % MAX_PROCS;
                highPriorityCount++;
                return true;
            case 1:
                if (lowPriorityCount == MAX_PROCS) return false;
                lowPriority[lowPriorityTail] = pid;
                lowPriorityTail = (lowPriorityTail + 1) % MAX_PROCS;
                lowPriorityCount++;
                return true;
            case 2:
                if (diskIoCount == MAX_PROCS) return false;
                ioDisk[diskIoTail] = pid;
                diskIoTail = (diskIoTail + 1) % MAX_PROCS;
                diskIoCount++;
                return true;
            case 3:
                if (tapeIoCount == MAX_PROCS) return false;
                ioTape[tapeIoTail] = pid;
                tapeIoTail = (tapeIoTail + 1) % MAX_PROCS;
                tapeIoCount++;
                return true;
            case 4:
                if (printerIoCount == MAX_PROCS) return false;
                ioPrinter[printerIoTail] = pid;
                printerIoTail = (printerIoTail + 1) % MAX_PROCS;
                printerIoCount++;
                return true;
        }
        return false;
    }

    static int popProcess(int fila) {
        switch (fila) {
            case 0:
                if (highPriorityCount == 0) return -1;
                int pidH = highPriority[highPriorityHead];
                highPriorityHead = (highPriorityHead + 1) % MAX_PROCS;
                highPriorityCount--;
                return pidH;
            case 1:
                if (lowPriorityCount == 0) return -1;
                int pidL = lowPriority[lowPriorityHead];
                lowPriorityHead = (lowPriorityHead + 1) % MAX_PROCS;
                lowPriorityCount--;
                return pidL;
            case 2:
                if (diskIoCount == 0) return -1;
                int pidD = ioDisk[diskIoHead];
                diskIoHead = (diskIoHead + 1) % MAX_PROCS;
                diskIoCount--;
                return pidD;
            case 3:
                if (tapeIoCount == 0) return -1;
                int pidT = ioTape[tapeIoHead];
                tapeIoHead = (tapeIoHead + 1) % MAX_PROCS;
                tapeIoCount--;
                return pidT;
            case 4:
                if (printerIoCount == 0) return -1;
                int pidP = ioPrinter[printerIoHead];
                printerIoHead = (printerIoHead + 1) % MAX_PROCS;
                printerIoCount--;
                return pidP;
        }
        return -1;
    }

    static int peekProcess(int fila) {
        switch (fila) {
            case 0: return highPriorityCount == 0 ? -1 : highPriority[highPriorityHead];
            case 1: return lowPriorityCount  == 0 ? -1 : lowPriority[lowPriorityHead];
            case 2: return diskIoCount       == 0 ? -1 : ioDisk[diskIoHead];
            case 3: return tapeIoCount       == 0 ? -1 : ioTape[tapeIoHead];
            case 4: return printerIoCount    == 0 ? -1 : ioPrinter[printerIoHead];
        }
        return -1;
    }

    // workload

    // gera bursts aleatórios para 1 processo e coloca na HIGH
    static void genProc(int i){
        // NOVO: preencher PID/PPID
        pid[i]  = i;
        ppid[i] = 0; // "init" (atende o PDF sem complicar)

        int bursts = BURSTS_MIN + R.nextInt(BURSTS_MAX - BURSTS_MIN + 1);
        nb[i] = bursts;

        for (int k=0; k<bursts; k++){
            cpuBurst[i][k] = CPU_MIN + R.nextInt(CPU_MAX - CPU_MIN + 1);
            if (k < bursts-1){
                int dev = 1 + R.nextInt(3); // 1=DISK, 2=TAPE, 3=PRINTER
                ioType[i][k] = dev;
                ioDur[i][k]  = IO_MIN + R.nextInt(IO_MAX - IO_MIN + 1);
            } else {
                ioType[i][k] = IO_NONE;
                ioDur[i][k]  = 0;
            }
        }

        status[i]=READY;
        priority[i]=1; // inicia na alta
        cur[i]=0;
        remainCpu[i]=cpuBurst[i][0];
        remainIo[i]=0;
        quantumLeft[i]=QUANTUM_HIGH;
        totalCpu[i]=0;

        push(0, i); // high
    }

    static void setupRandom(int n){
        for (int i=0; i<n; i++) genProc(i);
        dumpWorkload(n);
    }

    static void dumpWorkload(int n){
        System.out.println("== Workload (seed="+SEED+") ==");
        for(int i=0; i<n; i++){
            StringBuilder sb = new StringBuilder("PID "+pid[i]+" (PPID "+ppid[i]+") : ");
            for(int k=0;k<nb[i];k++){
                sb.append("CPU").append(cpuBurst[i][k]);
                if(ioType[i][k]==IO_DISK)         sb.append("->DISK").append(ioDur[i][k]).append("->");
                else if(ioType[i][k]==IO_TAPE)    sb.append("->TAPE").append(ioDur[i][k]).append("->");
                else if(ioType[i][k]==IO_PRINTER) sb.append("->PRINT").append(ioDur[i][k]).append("->");
            }
            if(sb.toString().endsWith("->")) sb.setLength(sb.length()-2);
            System.out.println(sb);
        }
    }

    // escalador / IO / CPU

    // escolhe processo: HIGH primeiro, depois LOW
    static void schedule() {
        if (running != -1) return;
        int p = popProcess(0);
        if (p != -1) {
            running = p; level = 1; status[p]=RUNNING; quantumLeft[p]=QUANTUM_HIGH;
            System.out.println("t="+ticks+" | SCHED HIGH -> pid="+p);
            return;
        }
        p = popProcess(1);
        if (p != -1) {
            running = p; level = 0; status[p]=RUNNING; quantumLeft[p]=QUANTUM_LOW;
            System.out.println("t="+ticks+" | SCHED LOW  -> pid="+p);
        }
    }

    // avança 1 tick de I/O no primeiro de cada fila de dispositivo
    static void stepIO() {
        if (diskIoCount > 0) {
            int p = ioDisk[diskIoHead];
            remainIo[p]--;
            if (remainIo[p] <= 0) {
                popProcess(2);
                status[p]=READY; priority[p]=0; // DISK volta baixa
                push(1, p);
                System.out.println("t="+ticks+" | IO_END DISK  pid="+p+" -> LOW");
            }
        }
        if (tapeIoCount > 0) {
            int p = ioTape[tapeIoHead];
            remainIo[p]--;
            if (remainIo[p] <= 0) {
                popProcess(3);
                status[p]=READY; priority[p]=1; // TAPE volta alta
                push(0, p);
                System.out.println("t="+ticks+" | IO_END TAPE  pid="+p+" -> HIGH");
            }
        }
        if (printerIoCount > 0) {
            int p = ioPrinter[printerIoHead];
            remainIo[p]--;
            if (remainIo[p] <= 0) {
                popProcess(4);
                status[p]=READY; priority[p]=1; // PRINTER volta alta
                push(0, p);
                System.out.println("t="+ticks+" | IO_END PRINT pid="+p+" -> HIGH");
            }
        }
    }

    // executa 1 tick de CPU do processo atual
    static void runTick() {
        // se estou rodando algo da LOW e chega alguém na HIGH, preempta
        if (running != -1 && level==0 && highPriorityCount > 0) {
            int p = running;
            status[p]=READY;
            push(1, p);
            running = -1;
            System.out.println("t="+ticks+" | PREEMPT pid="+p+" (LOW)");
            return;
        }

        if (running == -1) return;

        int p = running;
        remainCpu[p]--;
        quantumLeft[p]--;
        totalCpu[p]++;

        System.out.println("t="+ticks+" | RUN pid="+p+" (cpuRem="+remainCpu[p]+", q="+quantumLeft[p]+")");

        // fim de CPU burst
        if (remainCpu[p] <= 0) {
            int k = cur[p];
            int dev = ioType[p][k];

            if (dev == IO_NONE) { // acabou o processo
                status[p]=FINISHED; finished++; running=-1;
                tFinish[p] = ticks;
                System.out.println("t="+ticks+" | FINISH pid="+p);
                return;
            }

            // vai para I/O do burst k
            status[p]=BLOCKED;
            remainIo[p]=ioDur[p][k];

            // prepara próximo CPU (se existir)
            cur[p] = k+1;
            if (cur[p] < nb[p]) {
                remainCpu[p] = cpuBurst[p][cur[p]];
            }

            if (dev == IO_DISK) {
                push(2, p);
                System.out.println("t="+ticks+" | IO_START DISK  pid="+p+" dur="+remainIo[p]);
            } else if (dev == IO_TAPE) {
                push(3, p);
                System.out.println("t="+ticks+" | IO_START TAPE  pid="+p+" dur="+remainIo[p]);
            } else {
                push(4, p);
                System.out.println("t="+ticks+" | IO_START PRINT pid="+p+" dur="+remainIo[p]);
            }

            running=-1;
            return;
        }

        // acabou o quantum e ainda tem CPU
        if (quantumLeft[p] <= 0) {
            status[p]=READY;
            push(1, p); // vai pra LOW
            running=-1;
            System.out.println("t="+ticks+" | TIMEOUT pid="+p+" -> LOW (cpuRem="+remainCpu[p]+")");
        }
    }

    // métricas / log auxiliar
    static void accrueWaitingAll(){
        for(int i=0; i<NumberTest; i++){
            if(status[i]==READY) waitReady[i]++;
        }
    }

    static void printStats(){
        System.out.println("== Estatísticas ==");
        double sumTurn=0, sumWait=0;
        for(int i=0; i<NumberTest; i++){
            int turn = tFinish[i];
            sumTurn += turn;
            sumWait += waitReady[i];
            System.out.println("PID "+pid[i]+" (PPID "+ppid[i]+")"+
                    " | turnaround="+turn+
                    " | esperaReady="+waitReady[i]+
                    " | cpuTotal="+totalCpu[i]);
        }
        System.out.println("média turnaround="+(sumTurn/NumberTest));
        System.out.println("média esperaReady="+(sumWait/NumberTest));
    }

    public static void main(String[] args) {
        setupRandom(NumberTest);

        int MAX_TIME = 500;
        while (finished < NumberTest && ticks < MAX_TIME) {
            if (running == -1) schedule();
            stepIO();
            runTick();
            // printQueues(); 
            accrueWaitingAll();
            ticks++;
        }
        System.out.println("--- fim --- t="+ticks+" finished="+finished+"/"+NumberTest);
        printStats();
    }

    // opcional pra debug
    static void printQueues(){
        System.out.print("t="+ticks+" | HIGH:[");
        for(int i=0, idx=highPriorityHead; i<highPriorityCount; i++, idx=(idx+1)%MAX_PROCS)
            System.out.print(highPriority[idx] + (i<highPriorityCount-1?",":""));
        System.out.print("] LOW:[");
        for(int i=0, idx=lowPriorityHead; i<lowPriorityCount; i++, idx=(idx+1)%MAX_PROCS)
            System.out.print(lowPriority[idx] + (i<lowPriorityCount-1?",":""));
        System.out.print("] DISK:[");
        for(int i=0, idx=diskIoHead; i<diskIoCount; i++, idx=(idx+1)%MAX_PROCS)
            System.out.print(ioDisk[idx] + (i<diskIoCount-1?",":""));
        System.out.print("] TAPE:[");
        for(int i=0, idx=tapeIoHead; i<tapeIoCount; i++, idx=(idx+1)%MAX_PROCS)
            System.out.print(ioTape[idx] + (i<tapeIoCount-1?",":""));
        System.out.print("] PRINT:[");
        for(int i=0, idx=printerIoHead; i<printerIoCount; i++, idx=(idx+1)%MAX_PROCS)
            System.out.print(ioPrinter[idx] + (i<printerIoCount-1?",":""));
        System.out.println("]");
    }
}
