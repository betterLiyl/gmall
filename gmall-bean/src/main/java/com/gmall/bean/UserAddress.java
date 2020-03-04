package com.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
public class UserAddress {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    String id;
    @Column
    String userAddress;
    @Column
    String userId;
    @Column
    String consignee;
    @Column
    String phoneNum;
    @Column
    String isDefault;
}
