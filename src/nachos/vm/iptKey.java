package nachos.vm;

import java.util.Objects;

public class iptKey {
    Integer vpn;
    Integer processID;

    public iptKey(Integer vpn, Integer processID) {
        this.vpn = vpn;
        this.processID = processID;
    }

    public Integer getVpn() {
        return vpn;
    }

    public void setVpn(Integer vpn) {
        this.vpn = vpn;
    }

    public Integer getProcessID() {
        return processID;
    }

    public void setProcessID(Integer processID) {
        this.processID = processID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof iptKey)) return false;
        iptKey iptKey = (iptKey) o;
        return Objects.equals(vpn, iptKey.vpn) && Objects.equals(processID, iptKey.processID);
    }

    @Override
    public int hashCode() {
        return vpn * 1000 + processID;
    }
}
