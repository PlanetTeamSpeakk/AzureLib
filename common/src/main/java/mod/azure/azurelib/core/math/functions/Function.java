package mod.azure.azurelib.core.math.functions;

import mod.azure.azurelib.core.math.IValue;

/**
 * Abstract function class This class provides function capability (i.e. giving it arguments and upon {@link #get()}
 * method you receive output).
 */
public abstract class Function implements IValue {

    protected IValue[] args;

    protected String name;

    protected Function(IValue[] values, String name) throws Exception {
        if (values.length < this.getRequiredArguments()) {
            String message = String.format(
                    "Function '%s' requires at least %s arguments. %s are given!",
                    this.getName(),
                    this.getRequiredArguments(),
                    values.length
            );

            throw new Exception(message);
        }

        this.args = values;
        this.name = name;
    }

    /**
     * Get the value of nth argument
     */
    public double getArg(int index) {
        if (index < 0 || index >= this.args.length) {
            return 0;
        }

        return this.args[index].get();
    }

    @Override
    public String toString() {
        StringBuilder argsBuilder = new StringBuilder();

        for (int i = 0; i < this.args.length; i++) {
            argsBuilder.append(this.args[i].toString());

            if (i < this.args.length - 1) {
                argsBuilder.append(", ");
            }
        }

        return this.getName() + "(" + argsBuilder + ")";
    }

    /**
     * Get name of this function
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get minimum count of arguments this function needs
     */
    public int getRequiredArguments() {
        return 0;
    }
}
