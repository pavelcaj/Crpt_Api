package src.main.java;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Deque<Long> requestTimes;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestTimes = new ArrayDeque<>(requestLimit);
    }

    public void createDocument(String docJson, String signature) throws InterruptedException {
        synchronized (requestTimes) {
            cleanExpiredRequests();
            while (requestTimes.size() >= requestLimit) {
                long currentTime = System.currentTimeMillis();
                long nextAllowedTime = requestTimes.getFirst() + timeUnit.toMillis(1);
                long waitTime = nextAllowedTime - currentTime;
                if (waitTime > 0) {
                    requestTimes.wait(waitTime);
                    cleanExpiredRequests();
                }
            }
            requestTimes.addLast(System.currentTimeMillis());
        }


        Thread.sleep(1000);

        System.out.println("Document created successfully: " + docJson);
    }

    private void cleanExpiredRequests() {
        long currentTime = System.currentTimeMillis();
        while (!requestTimes.isEmpty() && (currentTime - requestTimes.getFirst() >= timeUnit.toMillis(1))) {
            requestTimes.removeFirst();
        }
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);


        try {
            api.createDocument("{ \"doc_id\": \"123\", \"doc_type\": \"LP_INTRODUCE_GOODS\" }", "signature");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted while waiting: " + e.getMessage());
        }
    }
}