package javax.management;

public class MBeanException extends JMException {
    private static final long serialVersionUID = 1L;
    private Exception exception;

    public MBeanException(Exception e) {
        super();
        this.exception = e;
    }

    public MBeanException(Exception e, String message) {
        super(message);
        this.exception = e;
    }

    public Exception getTargetException() {
        return exception;
    }

    @Override
    public Throwable getCause() {
        return exception;
    }
}
