/**
 * @Author: Chris Liang 1159696
 */

package message;

import java.io.Serializable;
import java.util.UUID;

public class Request<T extends Serializable> implements Serializable {
    private final UUID id;
    private String data;
    private T object;

    public Request() {
        this.id = UUID.randomUUID();
    }

    public Request(T object) {
        this.object = object;
        this.id = UUID.randomUUID();
    }

    public Request(String data, T object) {
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
