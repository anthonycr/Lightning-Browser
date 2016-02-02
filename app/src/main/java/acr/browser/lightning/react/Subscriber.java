package acr.browser.lightning.react;

public interface Subscriber<T> {
    void onComplete();

    void onNext(T item);
}
