alter table social_offers add column if not exists product_name_snapshot varchar(500);
alter table social_offers add column if not exists sku_snapshot varchar(64);
alter table social_offers add column if not exists unit_cost_snapshot numeric(12,2);
alter table social_offers add column if not exists expiry_snapshot date;
alter table social_offers add column if not exists transferred_at timestamp(6);
-- Historic values cannot be reconstructed reliably. Only capture fields for still-open offers.
update social_offers o set product_name_snapshot=p.name,sku_snapshot=p.sku,unit_cost_snapshot=p.unit_cost,expiry_snapshot=p.expiry_date
from products p where o.product_id=p.id and o.status='OPEN' and o.product_name_snapshot is null;
