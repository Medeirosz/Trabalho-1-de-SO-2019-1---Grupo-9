
package sistemas_operacionais_I.trabalho_1_so_gupo_9;

import java.util.*;

public class trabalho_1_so_grupo9 {

    //constantes
    static int MAX_PROCS = 6;
    static int MAX_BURSTS = 10;
    static int QUANTUM_HIGH = 4;
    static int QUANTUM_LOW = 1;

    //filas
    static int[] highPriority = new int[MAX_PROCS];
    static int[] lowPriority  = new int[MAX_PROCS];
    static int[] ioDisk       = new int[MAX_PROCS];
    static int[] ioTape       = new int[MAX_PROCS];
    static int[] ioPrinter    = new int[MAX_PROCS];


    //índices de cada fila p/ fila cirucular e controlar array
    static int highPriorityHead, highPriorityTail, highPriorityCount = 0;
    static int lowPriorityHead, lowPriorityTail, lowPriorityCount  = 0;
    static int diskIoHead, diskIoTail, diskIoCount = 0;
    static int tapeIoHead, tapeIoTail, tapeIoCount = 0;
    static int printerIoHead, printerIoTail, printerIoCount = 0;

    // funcao de inserir processo de uma fila
    public static boolean push(int fila, int processId) {
        switch (fila) {
            case 0: // HIGH
                if (highPriorityCount == MAX_PROCS) {
                 return false;   
                }

                highPriority[highPriorityTail] = processId;
                highPriorityTail = (highPriorityTail + 1) % MAX_PROCS;
                highPriorityCount++;
                return true;
    
            case 1: // LOW
                if (lowPriorityCount == MAX_PROCS) {
                    return false;
                }

                lowPriority[lowPriorityTail] = processId;
                lowPriorityTail = (lowPriorityTail + 1) % MAX_PROCS;
                lowPriorityCount++;
                return true;
                
            case 2: // DISK
                if (diskIoCount == MAX_PROCS) {
                    return false;
                }
                
                ioDisk[diskIoTail] = processId;
                diskIoTail = (diskIoTail + 1) % MAX_PROCS;
                diskIoCount++;
                return true;

            case 3: // TAPE
                if (tapeIoCount == MAX_PROCS) {
                    return false;
                }
                
                ioTape[tapeIoTail] = processId;
                tapeIoTail = (tapeIoTail + 1) % MAX_PROCS;
                tapeIoCount++;
                return true;

            case 4: // PRINTER
                if (printerIoCount == MAX_PROCS) {
                    return false;
                }
                
                ioPrinter[printerIoTail] = processId;
                printerIoTail = (printerIoTail + 1) % MAX_PROCS;
                printerIoCount++;
                return true;
            
        }
        return false;
    }

    // funcao de remover o ultimo processo de uma fila
    public static int popProcess(int fila) {
        switch (fila) {
            case 0: // HIGH
                if (highPriorityCount == 0) {
                    return -1; // fila vazia
                }
                int pidH = highPriority[highPriorityHead];
                highPriorityHead = (highPriorityHead + 1) % MAX_PROCS;
                highPriorityCount--;
                return pidH;
    
            case 1: // LOW
                if (lowPriorityCount == 0) return -1;
                int pidL = lowPriority[lowPriorityHead];
                lowPriorityHead = (lowPriorityHead + 1) % MAX_PROCS;
                lowPriorityCount--;
                return pidL;
    
            case 2: // DISK
                if (diskIoCount == 0) return -1;
                int processIdDisk = ioDisk[diskIoHead];
                diskIoHead = (diskIoHead + 1) % MAX_PROCS;
                diskIoCount--;
                return processIdDisk;

            case 3: // Tape
                if (tapeIoCount == 0) return -1;
                int processIdTape = ioTape[tapeIoHead];
                tapeIoHead = (tapeIoHead + 1) % MAX_PROCS;
                tapeIoCount--;
                return processIdTape;

            case 4: // Printer
                if (printerIoCount == 0) return -1;
                int processIdPrinter = ioTape[tapeIoHead];
                tapeIoHead = (tapeIoHead + 1) % MAX_PROCS;
                tapeIoCount--;
                return processIdPrinter;

        }
        return -1; 
    }

    // funcao de ver o proximo processo de uma fila
    public static void peekProcess(int fila) {
        
    }

 
    public static void main(String[] args) {
        
    }
}
