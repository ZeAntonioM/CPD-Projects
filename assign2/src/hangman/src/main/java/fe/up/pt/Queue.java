package fe.up.pt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Queue<T> {
    public int head, tail;
    private ReentrantLock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();
    private ArrayList<T> queue;

    public Queue() {
        this.head = this.tail = -1;
        this.queue = new ArrayList<>();
    }

    public void enqueue(T element) {
        lock.lock();
        try {
            queue.add(element);
            tail++;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T dequeue() {
        lock.lock();
        try {
            while (head == tail) {
                notEmpty.await();
            }
            head++;
            return queue.get(head);
        } catch (InterruptedException e) {
            return null;
        } finally {
            lock.unlock();
        }
    }
}

