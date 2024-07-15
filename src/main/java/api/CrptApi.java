package api;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private final TimeUnit timeUnit;
    private int requestLimit;
    private String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private HttpClient client;
    private final Gson gson;
    private Timer timer;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.gson = new Gson();
        this.timer = new Timer();
    }


    public void create(Doc document, String subscription) {

        timer.checkPermission();

            document.subscribe(new DescriptionImpl(subscription));
            String jsonDoc = gson.toJson(document);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDoc))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAcceptAsync(response -> System.out.println("Status Code: " + response.statusCode()));

    }

    private class Timer {
        private final LinkedBlockingQueue<LocalDateTime> timeQueue = new LinkedBlockingQueue<>(CrptApi.this.requestLimit);
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final ReentrantLock lock = new ReentrantLock(true);
        private final Condition condition = lock.newCondition();
        private final Semaphore semaphore;
        private final AtomicInteger permissions;

        public Timer() {
            this.permissions = new AtomicInteger(requestLimit);
            this.semaphore = new Semaphore(requestLimit, true);
        }

        public void put(LocalDateTime time) {
            timeQueue.offer(time);
        }

        private void checkPermission() {
            try {
                semaphore.acquire();
                lock.lock();
                timer.put(LocalDateTime.now());

                if (permissions.get() < 1) {
                    executor.submit(timer::signal);
                    condition.await();
                }
                permissions.decrementAndGet();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                semaphore.release();
            }
        }

        public void signal() {
            lock.lock();
            try {
                while (LocalDateTime.now().isBefore(timeQueue.peek().plus(1, timeUnit.toChronoUnit())))
                    Thread.sleep(Duration.of(1, timeUnit.toChronoUnit()).toMillis() / 10);
                timeQueue.clear();
                put(LocalDateTime.now());
                permissions.set(requestLimit);
                condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    public interface Doc{
        void subscribe(Description description);
    }

    public interface Description{}

    public interface Product{}

    public static class Document implements Doc {
        private Description description;
        private DocType doc_type;
        private String doc_id;
        private String doc_status;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private String reg_date;
        private String reg_number;
        private boolean importRequest;
        private Product[] products;

        public static class DocBuilder {
            private Description description;
            private DocType doc_type;
            private String doc_id;
            private String doc_status;
            private String owner_inn;
            private String participant_inn;
            private String producer_inn;
            private String production_date;
            private String production_type;
            private String reg_date;
            private String reg_number;
            private boolean importRequest = true;
            private Product[] products;

            public DocBuilder description(DescriptionImpl description) {
                this.description = description;
                return this;
            }

            public DocBuilder docType(DocType docType) {
                this.doc_type = docType;
                return this;
            }

            public DocBuilder docId(String docId) {
                this.doc_id = docId;
                return this;
            }

            public DocBuilder docStatus(String docStatus) {
                this.doc_status = docStatus;
                return this;
            }

            public DocBuilder ownerInn(String ownerInn) {
                this.owner_inn = ownerInn;
                return this;
            }

            public DocBuilder participantInn(String participantInn) {
                this.participant_inn = participantInn;
                return this;
            }

            public DocBuilder producerInn(String producerInn) {
                this.producer_inn = producerInn;
                return this;
            }

            public DocBuilder productionDate(String productionDate) {
                this.production_date = productionDate;
                return this;
            }

            public DocBuilder productionType(String productionType) {
                this.production_type = productionType;
                return this;
            }

            public DocBuilder regDate(String regDate) {
                this.reg_date = regDate;
                return this;
            }

            public DocBuilder regNumber(String regNumber) {
                this.reg_number = regNumber;
                return this;
            }

            public DocBuilder importRequest(boolean importRequest) {
                this.importRequest = importRequest;
                return this;
            }

            public DocBuilder products(Product[] products) {
                this.products = products;
                return this;
            }

            public Document build() {
                return new Document(this);
            }
        }

        private Document(DocBuilder builder) {
            this.description = builder.description;
            this.doc_type = builder.doc_type;
            this.doc_id = builder.doc_id;
            this.doc_status = builder.doc_status;
            this.owner_inn = builder.owner_inn;
            this.participant_inn = builder.participant_inn;
            this.producer_inn = builder.producer_inn;
            this.production_date = builder.production_date;
            this.production_type = builder.production_type;
            this.reg_date = builder.reg_date;
            this.reg_number = builder.reg_number;
            this.importRequest = builder.importRequest;
            this.products = builder.products;
        }

        public Description getDescription() {
            return description;
        }

        public void subscribe(Description description) {
            this.description = description;
        }

        public DocType getDoc_type() {
            return doc_type;
        }

        public void setDoc_type(DocType doc_type) {
            this.doc_type = doc_type;
        }

        public String getDoc_id() {
            return doc_id;
        }

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDoc_status() {
            return doc_status;
        }

        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public String getReg_date() {
            return reg_date;
        }

        public void setReg_date(String reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public Product[] getProducts() {
            return products;
        }

        public void setProducts(Product[] products) {
            this.products = products;
        }
    }

    public static class DescriptionImpl implements Description {
        private String participantInn;

        public DescriptionImpl(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    public enum DocType {
        LP_INTRODUCE_GOODS
    }

    public static class ProductImpl implements Product{
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        public static class ProductBuilder {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;

            public ProductBuilder certificateDocument(String certificateDocument) {
                this.certificate_document = certificateDocument;
                return this;
            }

            public ProductBuilder certificateDocumentDate(String certificateDocumentDate) {
                this.certificate_document_date = certificateDocumentDate;
                return this;
            }

            public ProductBuilder certificateDocumentNumber(String certificateDocumentNumber) {
                this.certificate_document_number = certificateDocumentNumber;
                return this;
            }

            public ProductBuilder ownerInn(String ownerInn) {
                this.owner_inn = ownerInn;
                return this;
            }

            public ProductBuilder producerInn(String producerInn) {
                this.producer_inn = producerInn;
                return this;
            }

            public ProductBuilder productionDate(String productionDate) {
                this.production_date = productionDate;
                return this;
            }

            public ProductBuilder tnvedCode(String tnvedCode) {
                this.tnved_code = tnvedCode;
                return this;
            }

            public ProductBuilder uitCode(String uitCode) {
                this.uit_code = uitCode;
                return this;
            }

            public ProductBuilder uituCode(String uituCode) {
                this.uitu_code = uituCode;
                return this;
            }

            public ProductImpl build() {
                return new ProductImpl(this);
            }
        }

        private ProductImpl(ProductBuilder builder) {
            this.certificate_document = builder.certificate_document;
            this.certificate_document_date = builder.certificate_document_date;
            this.certificate_document_number = builder.certificate_document_number;
            this.owner_inn = builder.owner_inn;
            this.producer_inn = builder.producer_inn;
            this.production_date = builder.production_date;
            this.tnved_code = builder.tnved_code;
            this.uit_code = builder.uit_code;
            this.uitu_code = builder.uitu_code;

        }

        public String getCertificate_document() {
            return certificate_document;
        }

        public void setCertificate_document(String certificate_document) {
            this.certificate_document = certificate_document;
        }

        public String getCertificate_document_date() {
            return certificate_document_date;
        }

        public void setCertificate_document_date(String certificate_document_date) {
            this.certificate_document_date = certificate_document_date;
        }

        public String getCertificate_document_number() {
            return certificate_document_number;
        }

        public void setCertificate_document_number(String certificate_document_number) {
            this.certificate_document_number = certificate_document_number;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getTnved_code() {
            return tnved_code;
        }

        public void setTnved_code(String tnved_code) {
            this.tnved_code = tnved_code;
        }

        public String getUit_code() {
            return uit_code;
        }

        public void setUit_code(String uit_code) {
            this.uit_code = uit_code;
        }

        public String getUitu_code() {
            return uitu_code;
        }

        public void setUitu_code(String uitu_code) {
            this.uitu_code = uitu_code;
        }
    }
}
