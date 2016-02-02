package acr.browser.lightning.react;

public interface Subscriber<T> {
    void onNext(T item);

    void onComplete();
}
