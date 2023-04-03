package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class UpdateNeighbor implements Serializable {
    private final InetSocketAddress right;
    private final InetSocketAddress left;

    public UpdateNeighbor(InetSocketAddress left, InetSocketAddress right) {
        this.left = left;
        this.right = right;
    }
    public InetSocketAddress getLeft() { return left;}
    public InetSocketAddress getRi  () { return right;}
}
