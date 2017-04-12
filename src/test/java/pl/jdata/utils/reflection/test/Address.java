package pl.jdata.utils.reflection.test;

import lombok.Data;

@Data
public class Address {

    private AddressType type;
    private String city;
    private String street;
    private int buildingNumber;
    private int apartmentNumber;

}
