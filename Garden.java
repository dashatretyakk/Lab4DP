package b;

import java.util.Date;
import java.util.concurrent.locks.*;
import java.io.*;

public class Garden {
    private final int SIZE = 10;
    private String[][] garden = new String[SIZE][SIZE];
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Початкова ініціалізація саду
    public Garden() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                garden[i][j] = "🌱";  // зелене рослинне
            }
        }
    }

    // Полив рослин
    public void waterPlants() {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (garden[i][j].equals("🥀")) {  // якщо рослина зів'яла
                        garden[i][j] = "🌱";
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Природа діє на рослини
    public void natureEffect() {
        lock.writeLock().lock();
        try {
            int i = (int) (Math.random() * SIZE);
            int j = (int) (Math.random() * SIZE);
            garden[i][j] = "🥀";  // зробити рослину зів'ялою
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Збереження стану саду в файл
    public void saveToFile() {
        lock.readLock().lock();
        try {
            FileWriter writer = new FileWriter("garden.txt", true);
            writer.write("\n" + new Date() + "\n");
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    writer.write(garden[i][j]);
                }
                writer.write("\n");
            }
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Виводить стан саду на екран
    public void displayGarden() {
        lock.readLock().lock();
        try {
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    System.out.print(garden[i][j]);
                }
                System.out.println();
            }
            System.out.println();
        } finally {
            lock.readLock().unlock();
        }
    }


    public static void main(String[] args) {
        Garden garden = new Garden();

        Thread gardener = new Thread(() -> {
            while (true) {
                garden.waterPlants();
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
            }
        });

        Thread nature = new Thread(() -> {
            while (true) {
                garden.natureEffect();
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }
        });

        Thread monitor1 = new Thread(() -> {
            while (true) {
                garden.saveToFile();
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
            }
        });

        Thread monitor2 = new Thread(() -> {
            while (true) {
                garden.displayGarden();
                try { Thread.sleep(500); } catch (InterruptedException e) {}
            }
        });

        gardener.start();
        nature.start();
        monitor1.start();
        monitor2.start();
    }
}
