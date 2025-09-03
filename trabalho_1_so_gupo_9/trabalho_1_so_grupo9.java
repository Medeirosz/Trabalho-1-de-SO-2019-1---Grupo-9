
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
    public static void push(int fila, int processId) {
        
    }

    // funcao de remover o ultimo processo de uma fila
    public static void popProcess(int fila) {
        
    }

 
    public static void main(String[] args) {
        
    }
}
