package message;

import java.io.Serializable;
import java.util.UUID;

public class Request implements Serializable {
    private final UUID id;
    private String data;
    private Object object;

    public Request() {
        this.id = UUID.randomUUID();
    }

    public Request(Object object) {
        this.object = object;
        this.id = UUID.randomUUID();
    }

    public Request(String data, Object object) {
        this.object = object;
        this.data = data;
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public String getIdentifier() {
        return id.toString();
    }

    public Object getObject() {
        return object;
    }
}
