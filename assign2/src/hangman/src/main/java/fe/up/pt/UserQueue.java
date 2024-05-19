package fe.up.pt;

public class UserQueue extends Queue<User> {
    private boolean isRanked;
    private int rank;

    public boolean ended = false;

    public UserQueue(boolean isRanked, int rank) {
        super();
        this.isRanked = isRanked;
        this.rank = rank;
    }

    public void enqueue(User user) {
        super.enqueue(user);
    }

    public User dequeue() {
        return super.dequeue();
    }

    public boolean isRanked() {
        return this.isRanked;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return this.rank;
    }

    public void setMeanRank() {
        int sum = 0;
        int i = 0;
        for (User user : this.queue) {
            sum += user.getRank();
            i++;
        }
        this.rank = sum / i;
    }

    public void start(){
        Thread.ofVirtual().start(new QueueHandler(this));
    }

    private class QueueHandler implements Runnable {
        private UserQueue queue;

        public QueueHandler(UserQueue queue) {
            this.queue = queue;
        }
        @Override
        public void run() {
            try {
                Thread.sleep(20000);
                queue.ended = true;
            } catch (InterruptedException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }


}
