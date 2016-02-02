package acr.browser.lightning.react;

public interface Subscription<T> {
    void onComplete();

    void onNext(T item);
}
