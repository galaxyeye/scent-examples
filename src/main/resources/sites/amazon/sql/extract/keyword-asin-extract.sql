-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 品类-关键词商品监控表
-- https://www.amazon.com/s?k=dresser&rh=n:1055398,n:1063306,n:1063308
-- CREATE TABLE `keyword_asin` (
--     `nodeID` VARCHAR(255) NULL COMMENT '品类ID', -- new add
--     `keyword` VARCHAR(255) NULL COMMENT '关键词',
--     `price` FLOAT  DEFAULT 0.0 COMMENT 'price',
--     `listprice` FLOAT  DEFAULT 0.0 COMMENT '挂牌价',
--     `title` varchar(255) DEFAULT NULL COMMENT '名称',
--     `pic` VARCHAR(255) NULL COMMENT '图片URL',
--     `score` FLOAT  DEFAULT 0.0 COMMENT '平均评星数',
--     `starnum` INT DEFAULT 0 COMMENT '评星总数',
--     `reviews` INT  DEFAULT 0 COMMENT '评论数',
--     `is_have_rating` tinyint DEFAULT 0 COMMENT '是否有评分数 -- 先用该字段来判断评论是否被清空',
--     `asin` VARCHAR(255) NULL COMMENT '商品asin编码',
--     `isac` tinyint DEFAULT 0 COMMENT '是否amazon推荐',
--     `isbs` tinyint DEFAULT 0 COMMENT '是否热卖排行榜',
--     `iscoupon` tinyint DEFAULT 0 COMMENT '是否使用优惠券',
--     `stock_status`  tinyint DEFAULT 0 COMMENT '是否缺货 0 不缺货  1  缺货  2 即将缺货 ',  -----  new add
--     `instock`  VARCHAR(255) NULL COMMENT '是否缺货 0 不缺货  1  缺货  2 即将缺货 ',  -----  new fix
--     `left_stock` int DEFAULT 0 COMMENT '剩余库存',  ---- new  add
--     `out_stock_time` VARCHAR(255) NULL COMMENT '即将缺货时间',  ---- new  add
--     `score5percent` VARCHAR(255) NULL COMMENT '5星级占比',-- new  add
--     `score4percent` VARCHAR(255) NULL COMMENT '4星级占比',-- new  add
--     `score3percent` VARCHAR(255) NULL COMMENT '3星级占比',-- new  add
--     `score2percent` VARCHAR(255) NULL COMMENT '2星级占比',-- new  add
--     `score1percent` VARCHAR(255) NULL COMMENT '1星级占比',-- new  add
--     `isad` tinyint DEFAULT 0 COMMENT '是否列表广告推广',-- new  add
--     `adposition_page` VARCHAR(255) NULL COMMENT '列表广告位置页码',-- new  add
--     `adposition_page_row` VARCHAR(255) NULL COMMENT '列表广告位置  页码行数',-- new  add
--     `is_recently` tinyint(4) DEFAULT '0' COMMENT '是否最新更新的数据',  -- new add
--     `createtime` VARCHAR(255) NULL COMMENT '更新日期'
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='关键词ASIN表';

select
    dom_base_uri(dom) as `url`,
    str_substring_after(dom_base_uri(dom), '&rh=') as `nodeID`,
    str_substring_between(dom_base_uri(dom), 'k=', '&') as `keyword`,
    dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
    dom_first_text(dom, 'a:has(span.a-price) span:containsOwn(/Item)') as `priceperitem`,
    dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
    dom_first_text(dom, 'h2 a') as `title`,
    dom_first_href(dom, 'h2 a') as `asin_url`,
    str_substring_between(dom_first_href(dom, 'h2 a'), '/dp/', '/ref=') as `asin`,
    dom_first_text(dom, 'div span[class]:containsOwn(by) ~ span[class~=a-color-secondary]') as `brand`,
    dom_first_text(dom, 'div a:containsOwn(new offers), div a span:containsOwn(new offers)') as `follow_seller_num`,

    dom_first_attr(dom, 'span[data-component-type=s-product-image] a img', 'src') as `pic`,
    dom_first_number(dom, 'div.a-section:expr(a>0) span[aria-label~=stars] i span', 0.0) as score,
    dom_first_text(dom, 'div.a-section:expr(a>0) span[aria-label~=stars] ~ span a') as `reviews`,
    dom_first_text(dom, 'div span[aria-label~=Amazon], div span:containsOwn(Amazon)') as `isac`,
    dom_first_text(dom, 'a[id~=BESTSELLER], div a[href~=bestsellers], div span:containsOwn(Best Seller)') as `isbs`,
    dom_first_text(dom, 'span[data-component-type=s-coupon-component]') as `iscoupon`,
    dom_first_text(dom, 'div.a-section span:contains(in stock), div.a-section span:contains(More Buying Choices)') as `instock`,
    dom_first_text(dom, 'div span:containsOwn(Sponsored)') as `isad`,
    dom_first_text(dom_owner_body(dom), 'ul.a-pagination li.a-selected') as `adposition_page`,
    (dom_top(dom) - dom_top(dom_select_first(dom_owner_body(dom), 'div.s-main-slot.s-result-list.s-search-results'))) / dom_height(dom) as `adposition_page_row`,
    dom_element_sibling_index(dom) as `position_in_list`,
    dom_width(dom_select_first(dom, 'a img[srcset]')) as `pic_width`,
    dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
from load_and_select(@url, 'div.s-main-slot.s-result-list.s-search-results > div:expr(img>0)');
