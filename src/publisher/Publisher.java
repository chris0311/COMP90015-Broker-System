package publisher;

public class Publisher {
    private String name;

    public Publisher(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void publish(String message) {
        System.out.println("Publisher " + name + " published message: " + message);
    }
}
