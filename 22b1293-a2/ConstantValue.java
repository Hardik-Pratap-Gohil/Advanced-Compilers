public class ConstantValue {
    public enum Type {
        INTEGER,
        BOOLEAN,
        UNKNOWN
    }

    public boolean isConstant;
    public Type type;
    public int intValue;
    public boolean boolValue;

    // Create a non-constant value
    public ConstantValue() {
        this.isConstant = false;
        this.type = Type.UNKNOWN;
    }

    // Create a constant integer value
    public ConstantValue(int value) {
        this.isConstant = true;
        this.type = Type.INTEGER;
        this.intValue = value;
    }

    // Create a constant boolean value
    public ConstantValue(boolean value) {
        this.isConstant = true;
        this.type = Type.BOOLEAN;
        this.boolValue = value;
    }

    // Copy constructor
    public ConstantValue(ConstantValue other) {
        this.isConstant = other.isConstant;
        this.type = other.type;
        this.intValue = other.intValue;
        this.boolValue = other.boolValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ConstantValue other = (ConstantValue) obj;
        if (!isConstant && !other.isConstant) return true;
        if (isConstant != other.isConstant) return false;
        if (type != other.type) return false;

        if (type == Type.INTEGER) {
            return intValue == other.intValue;
        } else if (type == Type.BOOLEAN) {
            return boolValue == other.boolValue;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = isConstant ? 1 : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + intValue;
        result = 31 * result + (boolValue ? 1 : 0);
        return result;
    }
}