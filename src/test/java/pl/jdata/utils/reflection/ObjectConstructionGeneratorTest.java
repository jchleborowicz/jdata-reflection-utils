package pl.jdata.utils.reflection;

import org.junit.Test;
import pl.jdata.utils.reflection.test.Home;

public class ObjectConstructionGeneratorTest {

    @Test
    public void generatesOutput() {
        ObjectConstructionGenerator.generate(Home.class);
    }

}
