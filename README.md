# Shiro-Session-Redis [![Build Status](https://travis-ci.org/izhangzhihao/Shiro-Session-Redis.svg?branch=master)](https://travis-ci.org/izhangzhihao/Shiro-Session-Redis)

    实现将Shiro的session存储在Redis中，并可配置ehcache作为进程内缓存，通过redis消息订阅发布实现session缓存失效等

# 主要的后端架构：

    Spring + Spring MVC + Apache Shiro + Spring Session + Spring Data Redis + Jedis 
    
# 所需的环境和正确打开项目的姿势 可以参考SpringMVCSeedProject [![Build Status](http://210.31.198.175:8080/jenkins/job/SpringMVCSeedProject/badge/icon)](https://github.com/izhangzhihao/SpringMVCSeedProject)完整配置

    运行项目前请确保有一个Redis实例在运行，并且正确配置(src/main/resources/redis.properties)
    可以参考下面的配置实例

# 配置Lombok :

    https://izhangzhihao.github.io/2016/07/29/使用Lombok注解

# Spring-Shiro.xml 主要配置：

---
``` XML

    <!--安全管理器-->
    <bean id="securityManager"
          class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
        <property name="realm" ref="shiroRealm"/>
        <property name="rememberMeManager" ref="rememberMeManager"/>

        <!--可选项 默认使用ServletContainerSessionManager，直接使用容器的HttpSession，可以通过配置sessionManager，使用DefaultWebSessionManager来替代-->
        <property name="sessionManager" ref="sessionManager"/>
        <!--可选项 最好使用;SessionDao 中 doReadSession 读取过于频繁了-->
        <property name="cacheManager" ref="shiroEhcacheManager"/>
    </bean>
    
    <!--session管理器-->
    <bean id="sessionManager"
          class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
        <!-- 设置全局会话超时时间，默认30分钟(1800000) -->
        <property name="globalSessionTimeout" value="1800000"/>
        <!-- 是否在会话过期后会调用SessionDAO的delete方法删除会话 默认true-->
        <property name="deleteInvalidSessions" value="false"/>
        <!-- 是否开启会话验证器任务 默认true -->
        <property name="sessionValidationSchedulerEnabled" value="false"/>
        <!-- 会话验证器调度时间 -->
        <property name="sessionValidationInterval" value="1800000"/>
        <property name="sessionFactory" ref="sessionFactory"/>
        <property name="sessionDAO" ref="cachingShiroSessionDao"/>
        <!-- 默认JSESSIONID，同tomcat/jetty在cookie中缓存标识相同，修改用于防止访问404页面时，容器生成的标识把shiro的覆盖掉 -->
        <property name="sessionIdCookie">
            <bean class="org.apache.shiro.web.servlet.SimpleCookie">
                <constructor-arg name="name" value="SHRIOSESSIONID"/>
            </bean>
        </property>
        <property name="sessionListeners">
            <list>
                <ref bean="sessionListener"/>
            </list>
        </property>
    </bean>


    <!-- 自定义Session工厂方法 返回会标识是否修改主要字段的自定义Session-->
    <bean id="sessionFactory"
          class="com.github.izhangzhihao.ShiroSessionOnRedis.Session.ShiroSessionFactory"/>


    <!--缓存管理器-->
    <!-- 用户授权信息Cache, 采用EhCache，本地缓存最长时间应比中央缓存时间短一些，以确保Session中doReadSession方法调用时更新中央缓存过期时间 -->
    <bean id="shiroEhcacheManager" class="org.apache.shiro.cache.ehcache.EhCacheManager"
          p:cacheManagerConfigFile="classpath:ehcache-shiro.xml"/>


    <bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>


    <bean id="sessionListener"
          class="com.github.izhangzhihao.ShiroSessionOnRedis.Listener.ShiroSessionListener"
          p:sessionDao-ref="cachingShiroSessionDao"
          p:shiroSessionService-ref="shiroSessionService"/>


    <bean id="cachingShiroSessionDao"
          class="com.github.izhangzhihao.ShiroSessionOnRedis.Repository.CachingShiroSessionDao"
          p:sessionRepository-ref="shiroSessionRepository"/>


    <bean id="shiroSessionRepository"
          class="com.github.izhangzhihao.ShiroSessionOnRedis.Repository.ShiroSessionRepository"
          p:redisTemplate-ref="redisTemplate"/>


    <bean id="shiroSessionService"
          class="com.github.izhangzhihao.ShiroSessionOnRedis.Service.ShiroSessionService"
          p:redisTemplate-ref="redisTemplate"
          p:sessionDao-ref="cachingShiroSessionDao"/>
```
---


# Spring-Redis.xml 主要配置：


---
``` XML

   <bean id="poolConfig" class="redis.clients.jedis.JedisPoolConfig"
          p:maxIdle="${redis.maxIdle}"
          p:maxTotal="${redis.maxTotal}"
          p:maxWaitMillis="${redis.maxWaitMillis}"
          p:testOnBorrow="${redis.testOnBorrow}"/>

    <bean id="connectionFactory" class="org.springframework.data.redis.connection.jedis.JedisConnectionFactory"
          p:hostName="${redis.host}"
          p:port="${redis.port}"
          p:password="${redis.passWord}"
          p:timeout="${redis.timeout}"
          p:database="${redis.database}"
          p:poolConfig-ref="poolConfig"/>

    <bean id="redisTemplate" class="org.springframework.data.redis.core.StringRedisTemplate">
        <property name="connectionFactory" ref="connectionFactory"/>
    </bean>

    <redis:listener-container connection-factory="connectionFactory">
        <!-- the method attribute can be skipped as the default method name is "handleMessage" -->
        <redis:listener ref="shiroSessionService" topic="shiro.session.uncache" serializer="redisSerializer"/>
    </redis:listener-container>
    
```
---

