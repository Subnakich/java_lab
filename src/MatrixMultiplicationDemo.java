import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;

public class MatrixMultiplicationDemo {

    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final ForkJoinPool pool = new ForkJoinPool();
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

    public static void main(String[] args) {

        // Размер матрицы (N x N)
        final int N = 500;
        // Максимальное случайное значение
        final int maxRandomValue = 100;
        Random random = new Random();

        // Инициализация матрицы matrix1 случайными значениями
        double[][] matrix1 = new double[N][N];
        for (int i = 0; i < matrix1.length; i++)
            for (int j = 0; j < matrix1[i].length; j++)
                matrix1[i][j] = random.nextInt(maxRandomValue + 1);

        // Инициализация матрицы matrix2 случайными значениями
        double[][] matrix2 = new double[N][N];
        for (int i = 0; i < matrix2.length; i++)
            for (int j = 0; j < matrix2[i].length; j++)
                matrix2[i][j] = random.nextInt(maxRandomValue + 1);

        // Умножение матриц последовательно
        long startTime = System.currentTimeMillis();
        double[][] result = multiplyMatrixSequential(matrix1, matrix2);
        long endTime = System.currentTimeMillis();
        System.out.println("Sequential time is " + (endTime - startTime)
                + " milliseconds");

        // Умножение матриц с использованием Fork/Join пула
        startTime = System.currentTimeMillis();
        result = multiplyMatrixForkJoinPool(matrix1, matrix2);
        endTime = System.currentTimeMillis();
        System.out.println("Fork/Join pool time is " + (endTime - startTime)
                + " milliseconds");

        // Освобождаем ресурсы
        pool.shutdown();

        // Умножение матриц с использованием пула потоков
        startTime = System.currentTimeMillis();
        result = multiplyMatrixThreadPool(matrix1, matrix2);
        endTime = System.currentTimeMillis();
        System.out.println("Thread Pool time is " + (endTime - startTime) + " milliseconds");

    }

    // Метод для последовательного умножения матриц
    public static double[][] multiplyMatrixSequential(double[][] a, double[][] b) {
        double[][] result = new double[a.length][b[0].length];
        for (int i = 0; i < result.length; i++)
            for (int j = 0; j < result[0].length; j++)
                for (int k = 0; k < a[0].length; k++)
                    result[i][j] += a[i][k] * b[k][j];

        return result;
    }

    // Метод для умножения матриц с использованием Fork/Join пула
    public static double[][] multiplyMatrixForkJoinPool(double[][] a, double[][] b) {
        double[][] result = new double[a.length][b[0].length];
        RecursiveAction task = new RecursiveMultiply(a, b, result);

        pool.invoke(task);
        return result;
    }

    // Класс для выполнения умножения матриц внутри Fork/Join задачи
    private static class RecursiveMultiply extends RecursiveAction {
        private double[][] a;
        private double[][] b;
        private double[][] c;

        public RecursiveMultiply(double[][] a, double[][] b, double[][] c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public void compute() {
            ArrayList<RecursiveAction> tasks = new ArrayList<RecursiveAction>();
            for (int i = 0; i < c.length; i++)
                for (int j = 0; j < c[0].length; j++)
                    tasks.add(new RecursiveMultiply.MultiplyOneRow(i, j));

            invokeAll(tasks);
        }

        // Внутренний класс для выполнения умножения одной строки матрицы
        public class MultiplyOneRow extends RecursiveAction {
            int i;
            int j;

            public MultiplyOneRow(int i, int j) {
                this.i = i;
                this.j = j;
            }

            @Override
            public void compute() {
                c[i][j] = 0;
                for (int k = 0; k < a[0].length; k++)
                    c[i][j] += a[i][k] * b[k][j];
            }
        }
    }

    // Метод для умножения матриц с использованием пула потоков
    public static double[][] multiplyMatrixThreadPool(double[][] a, double[][] b) {
        int numRowsA = a.length;
        int numColsA = a[0].length;
        int numColsB = b[0].length;
        double[][] result = new double[numRowsA][numColsB];

        for (int i = 0; i < numRowsA; i++) {
            final int row = i;
            threadPool.submit(() -> {
                for (int j = 0; j < numColsB; j++) {
                    for (int k = 0; k < numColsA; k++) {
                        result[row][j] += a[row][k] * b[k][j];
                    }
                }
            });
        }

        // Ожидаем завершения всех задач
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result;
    }

}
