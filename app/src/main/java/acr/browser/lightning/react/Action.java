package acr.browser.lightning.react;

public interface Action<T> {
    void onSubscribe(Subscriber<T> subscriber);
}
