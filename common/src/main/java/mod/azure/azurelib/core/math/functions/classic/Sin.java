package mod.azure.azurelib.core.math.functions.classic;

import mod.azure.azurelib.core.math.IValue;
import mod.azure.azurelib.core.math.functions.Function;

public class Sin extends Function {

    public Sin(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override
    public int getRequiredArguments() {
        return 1;
    }

    @Override
    public double get() {
        return Math.sin(this.getArg(0));
    }
}
