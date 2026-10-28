create table smart_pricing_settings (
 id bigint primary key, version bigint not null default 0,
 mode varchar(20) not null default 'RECOMMEND', maximum_discount integer not null default 20,
 cooldown_hours integer not null default 24, allow_low_confidence boolean not null default false,
 allow_restore boolean not null default false, allowed_categories_json text not null default '[]',
 updated_at timestamp not null,
 constraint ck_smart_mode check(mode in ('RECOMMEND','AUTOPILOT')),
 constraint ck_smart_limits check(maximum_discount between 0 and 90 and cooldown_hours between 1 and 720)
);
insert into smart_pricing_settings(id,version,mode,maximum_discount,cooldown_hours,allow_low_confidence,allow_restore,allowed_categories_json,updated_at)
values(1,0,'RECOMMEND',20,24,false,false,'[]',current_timestamp);
