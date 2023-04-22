-- auto-generated definition
create table user
(
    id           bigint auto_increment comment '用户 ID'
        primary key,
    username     varchar(256)                       null comment '用户名',
    avatarUrl    varchar(1024)                      null comment '用户头像',
    gender       tinyint                            null comment '性别',
    userPassword varchar(512)                       not null comment '用户密码',
    userAccount  varchar(512)                       null comment '用户账号',
    phone        varchar(128)                       null comment '电话',
    email        varchar(128)                       null comment '邮箱',
    userStatus   int      default 0                 not null comment '用户状态',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '逻辑删除',
    userRole     int      default 0                 not null comment '用户角色  0 - 普通用户  1 - 管理员',
    planetCode   varchar(512)                       null comment '星球编号',
    tags         varchar(1024)                      null comment '标签列表',
    constraint userAccount
        unique (userAccount)
)
    comment '用户表';

-- auto-generated definition
create table tag
(
    id         bigint auto_increment comment 'id'
        primary key,
    tagName    varchar(256)                       null comment '标签名称',
    userId     bigint                             null comment '用户 id',
    parentId   bigint                             null comment '父标签 id',
    isParent   tinyint                            null comment '0-不是1-是',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 null comment '是否删除',
    constraint uniIdx_tagName
        unique (tagName)
)comment '标签';

create index idx_userId
    on tag (userId);

