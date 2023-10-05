package a;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;

class MyReaderWriterLock {
    private int readers = 0;
    private int writers = 0;
    private int writeRequests = 0;

    public synchronized void lockRead() throws InterruptedException {
        while (writers > 0 || writeRequests > 0) {
            wait();
        }
        readers++;
    }

    public synchronized void unlockRead() {
        readers--;
        notifyAll();
    }

    public synchronized void lockWrite() throws InterruptedException {
        writeRequests++;
        while (readers > 0 || writers > 0) {
            wait();
        }
        writeRequests--;
        writers++;
    }

    public synchronized void unlockWrite() {
        writers--;
        notifyAll();
    }
}

class Database {
    private final Path path;
    private final MyReaderWriterLock lock = new MyReaderWriterLock();

    public Database(String filePath) {
        this.path = Paths.get(filePath);
    }

    public String findPhoneByName(String name) throws IOException, InterruptedException {
        lock.lockRead();
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] parts = line.split(" - ");
                if (parts[0].equals(name)) {
                    return parts[1];
                }
            }
            return null;
        } finally {
            lock.unlockRead();
        }
    }

    public String findNameByPhone(String phone) throws IOException, InterruptedException {
        lock.lockRead();
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] parts = line.split(" - ");
                if (parts[1].equals(phone)) {
                    return parts[0];
                }
            }
            return null;
        } finally {
            lock.unlockRead();
        }
    }

    public void addEntry(String name, String phone) throws IOException, InterruptedException {
        lock.lockWrite();
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(path));
            lines.add(name + " - " + phone);
            Files.write(path, lines);
        } finally {
            lock.unlockWrite();
        }
    }

    public void removeEntry(String name) throws IOException, InterruptedException {
        lock.lockWrite();
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(path));
            lines.removeIf(line -> line.startsWith(name + " - "));
            Files.write(path, lines);
        } finally {
            lock.unlockWrite();
        }
    }
}

public class MultiThreadedDatabaseAccess {
    public static void main(String[] args) {
        Database db = new Database("database.txt");

        // Створимо або перепишемо database.txt перед початком
        try (FileWriter fw = new FileWriter("database.txt", false);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(""); // очистимо файл
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Заповнимо базу даних
        try {
            db.addEntry("П.І.Б.1", "11111111");
            db.addEntry("П.І.Б.2", "22222222");
            db.addEntry("П.І.Б.3", "33333333");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Потік, який знаходить телефон за прізвищем
        Thread reader1 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                try {
                    String name = "П.І.Б." + i;
                    System.out.println("Потік читача 1: шукаємо телефон для " + name);
                    String phone = db.findPhoneByName(name);
                    if (phone != null) {
                        System.out.println("Потік читача 1: знайдено телефон " + phone + " для " + name);
                    } else {
                        System.out.println("Потік читача 1: не вдалося знайти телефон для " + name);
                    }
                    Thread.sleep(1000); // Затримка в 1 секунду
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Потік, який знаходить ім'я за телефоном
        Thread reader2 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                try {
                    String phone = "4444444-" + i;
                    System.out.println("Потік читача 2: шукаємо ім'я за телефоном " + phone);
                    String name = db.findNameByPhone(phone);
                    if (name != null) {
                        System.out.println("Потік читача 2: знайдено ім'я " + name + " за телефоном " + phone);
                    } else {
                        System.out.println("Потік читача 2: не вдалося знайти ім'я за телефоном " + phone);
                    }
                    Thread.sleep(1500); // Затримка в 1.5 секунди
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Потік, який додає запис
        Thread writer1 = new Thread(() -> {
            for (int i = 1; i <= 6; i++) {
                try {
                    String name = "П.І.Б." + i;
                    String phone = "4444444-" + i;
                    System.out.println("Потік письменника 1: додаємо " + name + " з телефоном " + phone);
                    db.addEntry(name, phone);
                    System.out.println("Потік письменника 1: запис додано: " + name + " з телефоном " + phone);
                    Thread.sleep(2000); // Затримка в 2 секунди
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Потік, який видаляє запис
        Thread writer2 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                try {
                    String name = "П.І.Б." + i;
                    System.out.println("Потік письменника 2: видаляємо запис для " + name);
                    db.removeEntry(name);
                    System.out.println("Потік письменника 2: запис видалено для " + name);
                    Thread.sleep(2500); // Затримка в 2.5 секунди
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Запускаємо всі потоки
        reader1.start();
        reader2.start();
        writer1.start();
        writer2.start();

        // Чекаємо завершення всіх потоків перед завершенням main
        try {
            reader1.join();
            reader2.join();
            writer1.join();
            writer2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}