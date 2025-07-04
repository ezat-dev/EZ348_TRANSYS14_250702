package com.transys.dao;

import java.util.List;

import javax.annotation.Resource;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.transys.domain.Alarm;


@Repository
public class AlarmDaoImpl implements AlarmDao{
	    
		@Resource(name="sessionSQLite")
		private SqlSession sessionSQLite;
		
	    @Resource(name="session")
	    private SqlSession sqlSession;


	    @Override
	    public List<Alarm> getAlarmSummary(Alarm params) {
	        return sessionSQLite.selectList("alarm.getAlarmSummary", params);  
	    }
	    @Override
	    public List<Alarm> getAlarmList(Alarm params) {
	        return sessionSQLite.selectList("alarm.getAlarmList", params);  
	    }
	    @Override
	    public List<Alarm> getAlarmInfo(Alarm alarm) {
	        return sqlSession.selectList("alarm.getAlarmInfo", alarm);  
	    }

}
