package nachos.vm;

import java.util.Objects;

public class iptKey {
    int vpn;
    int processID;

    public iptKey(int vpn, int processID) {
        this.vpn = vpn;
        this.processID = processID;
    }

    public int getVpn() {
        return vpn;
    }

    public void setVpn(int vpn) {
        this.vpn = vpn;
    }

    public int getProcessID() {
        return processID;
    }

    public void setProcessID(int processID) {
        this.processID = processID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof iptKey)) return false;
        iptKey iptKey = (iptKey) o;
        return getVpn() == iptKey.getVpn() && getProcessID() == iptKey.getProcessID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVpn(), getProcessID());
    }
}
