package pl.jdata.utils.reflection.test;

import lombok.Data;

import java.util.Set;

@Data
public class Home {

    private Address address;
    private String type;
    private Set<String> colors;

}
