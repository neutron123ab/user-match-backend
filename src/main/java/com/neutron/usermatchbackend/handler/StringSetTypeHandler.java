package com.neutron.usermatchbackend.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.util.CollectionUtils;

/**
 * @author zzs
 * @date 2023/3/27 21:10
 */
public class StringSetTypeHandler extends BaseTypeHandler<Set<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, Set<String> strings, JdbcType jdbcType) throws SQLException {
        Gson gson = new Gson();
        String content = CollectionUtils.isEmpty(strings) ? null : gson.toJson(strings);
        preparedStatement.setString(i, content);
    }

    @Override
    public Set<String> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        Gson gson = new Gson();
        String string = resultSet.getString(s);
        return StringUtils.isBlank(string) ? new HashSet<>() : gson.fromJson(string, new TypeToken<Set<String>>() {}.getType());
    }

    @Override
    public Set<String> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        Gson gson = new Gson();
        String string = resultSet.getString(i);
        return StringUtils.isBlank(string) ? new HashSet<>() : gson.fromJson(string, new TypeToken<Set<String>>() {}.getType());
    }

    @Override
    public Set<String> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        Gson gson = new Gson();
        String string = callableStatement.getString(i);
        return StringUtils.isBlank(string) ? new HashSet<>() : gson.fromJson(string, new TypeToken<Set<String>>() {}.getType());
    }
}

