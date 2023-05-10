import mpi.MPI;

public class CollectiveMatrixMultiplicator {
    public static int NUMBER_OF_ROWS_IN_A = 1000;
    public static int NUMBER_OF_COLS_IN_A = 1000;
    public static int NUMBER_OF_COLS_IN_B = 1000;
    public static int MASTER = 0;

    public static boolean RESULT_IS_PRINTED = false;
    public static boolean RANDOMIZE_MATRICES = true;
    public static boolean VALIDATE_RESULT = true;

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
            if (RANDOMIZE_MATRICES) {
                Helper.initializeMatrixWithRandom(a, 50, 500);
                Helper.initializeMatrixWithRandom(b, 50, 500);
            } else {
                Helper.initializeMatrixWithNumber(a, 100);
                Helper.initializeMatrixWithNumber(b, 100);
            }
            startTime = System.nanoTime();
        }

        int rowsPerTask = NUMBER_OF_ROWS_IN_A / tasksNumber;
        int extraRows = NUMBER_OF_ROWS_IN_A % tasksNumber;

        var rowsCounts = new int[tasksNumber];
        var rowsOffsets = new int[tasksNumber];
        for (int i = 0; i < tasksNumber; i++) {
            rowsCounts[i] = i < extraRows ? rowsPerTask + 1 : rowsPerTask;
            rowsOffsets[i] = i == 0 ? 0 : rowsOffsets[i - 1] + rowsCounts[i - 1];
        }
        var rowsInTask = rowsCounts[taskId];

        var aRowsBuffer = new double[rowsInTask][NUMBER_OF_COLS_IN_A];
        MPI.COMM_WORLD.Scatterv(
                a,
                0,
                rowsCounts,
                rowsOffsets,
                MPI.OBJECT,
                aRowsBuffer,
                0,
                rowsInTask,
                MPI.OBJECT,
                MASTER
        );

        MPI.COMM_WORLD.Bcast(b, 0, NUMBER_OF_COLS_IN_A, MPI.OBJECT, MASTER);
        var cRowsBuffer = new double[rowsInTask][NUMBER_OF_COLS_IN_B];
        for (int k = 0; k < NUMBER_OF_COLS_IN_B; k++) {
            for (int i = 0; i < rowsInTask; i++) {
                for (int j = 0; j < NUMBER_OF_COLS_IN_A; j++) {
                    cRowsBuffer[i][k] += aRowsBuffer[i][j] * b[j][k];
                }
            }
        }

        MPI.COMM_WORLD.Gatherv(
                cRowsBuffer,
                0, rowsInTask,
                MPI.OBJECT,
                c,
                0,
                rowsCounts,
                rowsOffsets,
                MPI.OBJECT,
                MASTER
        );

        if (taskId == MASTER) {
            endTime = System.nanoTime();
            System.out.println("===== RESULTS =====");
            if (RESULT_IS_PRINTED) {
                Helper.outputMatrix(c);
            }
            System.out.println("Execution time: " + (endTime - startTime) + " ns");
            if (VALIDATE_RESULT) {
                System.out.println("Result is valid: " + NaiveMatrixMultiplicator.validateMultiplicationResult(a, b, c));
            }
        }
        MPI.Finalize();
    }
}