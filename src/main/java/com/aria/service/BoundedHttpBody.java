package com.aria.service;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
public final class BoundedHttpBody implements HttpResponse.BodySubscriber<byte[]> {
 private final HttpResponse.BodySubscriber<byte[]> delegate=HttpResponse.BodySubscribers.ofByteArray();
 private final int maximum;private int size;private Flow.Subscription subscription;
 public BoundedHttpBody(int maximum){this.maximum=maximum;}
 public CompletionStage<byte[]> getBody(){return delegate.getBody();}
 public void onSubscribe(Flow.Subscription value){subscription=value;delegate.onSubscribe(value);}
 public void onNext(List<ByteBuffer> buffers){for(var b:buffers)size+=b.remaining();if(size>maximum){subscription.cancel();delegate.onError(new java.io.IOException("Response exceeds the allowed size"));}else delegate.onNext(buffers);}
 public void onError(Throwable t){delegate.onError(t);}public void onComplete(){delegate.onComplete();}
}
