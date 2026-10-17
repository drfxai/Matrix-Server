package javax.management;

public class JMRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JMRuntimeException() {
        super();
    }

    public JMRuntimeException(String msg) {
        super(msg);
    }
}
