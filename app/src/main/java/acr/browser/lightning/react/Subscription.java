package acr.browser.lightning.react;

public interface Subscription<T> {
    void onNext(T item);

    void onComplete();
}
