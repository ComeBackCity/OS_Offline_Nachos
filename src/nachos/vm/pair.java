package nachos.vm;

public class pair <type1, type2>{
    type1 element1;
    type2 element2;

    public pair(type1 element1, type2 element2) {
        this.element1 = element1;
        this.element2 = element2;
    }

    public type1 getElement1() {
        return element1;
    }

    public void setElement1(type1 element1) {
        this.element1 = element1;
    }

    public type2 getElement2() {
        return element2;
    }

    public void setElement2(type2 element2) {
        this.element2 = element2;
    }
}
