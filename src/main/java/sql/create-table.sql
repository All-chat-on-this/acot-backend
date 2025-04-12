-- 创建用户表
CREATE TABLE user
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50) NOT NULL,
    password    VARCHAR(255),
    nickname    VARCHAR(50),
    login_type  TINYINT     NOT NULL,
    open_id     VARCHAR(100),
    token       VARCHAR(255),
    create_time DATETIME    NOT NULL,
    update_time DATETIME,
    is_deleted  TINYINT     NOT NULL DEFAULT 0
);

-- 创建用户配置表
CREATE TABLE user_config
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    config_key   VARCHAR(100) NOT NULL,
    config_value TEXT         NOT NULL,
    create_time  DATETIME     NOT NULL,
    update_time  DATETIME,
    is_deleted   TINYINT      NOT NULL DEFAULT 0
);

-- 创建对话表
CREATE TABLE conversation
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL,
    create_time DATETIME     NOT NULL,
    update_time DATETIME,
    is_deleted  TINYINT      NOT NULL DEFAULT 0
);

-- 创建对话消息表
CREATE TABLE conversation_message
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL,
    role            VARCHAR(50) NOT NULL,
    content         TEXT,
    thinking_text   TEXT,
    create_time     DATETIME    NOT NULL,
    update_time     DATETIME,
    is_deleted      TINYINT     NOT NULL DEFAULT 0
);