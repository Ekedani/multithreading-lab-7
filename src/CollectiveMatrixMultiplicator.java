import mpi.MPI;

public class CollectiveMatrixMultiplicator {
    public static int NUMBER_OF_ROWS_IN_A = 1000;
    public static int NUMBER_OF_COLS_IN_A = 1000;
    public static int NUMBER_OF_COLS_IN_B = 1000;
    public static int MASTER = 0;

    public static boolean RESULT_IS_PRINTED = true;

    public static void main(String[] args) {
        int taskId, tasksNumber;

        double[][] a = new double[NUMBER_OF_ROWS_IN_A][NUMBER_OF_COLS_IN_A];
        double[][] b = new double[NUMBER_OF_COLS_IN_A][NUMBER_OF_COLS_IN_B];
        double[][] c = new double[NUMBER_OF_ROWS_IN_A][NUMBER_OF_COLS_IN_B];

        MPI.Init(args);
        taskId = MPI.COMM_WORLD.Rank();
        tasksNumber = MPI.COMM_WORLD.Size();
        long startTime = 0, endTime;
        if (taskId == MASTER) {
            System.out.println("MPI_CMM has started with " + tasksNumber + " tasks.");
            Helper.initializeMatrixWithNumber(a, 10);
            Helper.initializeMatrixWithNumber(b, 10);
            startTime = System.nanoTime();
        }

        // MPI.COMM_WORLD.Scatter();
        MPI.COMM_WORLD.Bcast(b, 0, NUMBER_OF_COLS_IN_A, MPI.OBJECT, MASTER);
        /*for (int k = 0; k < NUMBER_OF_COLS_IN_B; k++) {
            for (int i = 0; i < rows[0]; i++) {
                for (int j = 0; j < NUMBER_OF_COLS_IN_A; j++) {
                    c[i][k] += a[i][j] * b[j][k];
                }
            }
        }*/
        // MPI.COMM_WORLD.Gather();

        if (taskId == MASTER) {
            endTime = System.nanoTime();
            System.out.println("===== RESULTS =====");
            if (RESULT_IS_PRINTED) {
                Helper.outputMatrix(c);
            }
            System.out.println("Execution time: " + (endTime - startTime) + " ns");
        }
        MPI.Finalize();
    }
}