package com.gmall.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
@Data
@NoArgsConstructor
public class BaseAttrValue implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    @Column
    private String valueName;
    @Column
    private String attrId;
    @Transient
    private String urlParam;
}

