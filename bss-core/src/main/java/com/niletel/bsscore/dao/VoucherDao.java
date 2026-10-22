package com.niletel.bsscore.dao;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;

@ApplicationScoped
public class VoucherDao {

    @Resource(lookup = "java:jboss/datasources/BssCoreDS")
    private DataSource dataSource;

}
