-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_base_uri(dom) as `url`,
    dom_first_slim_html(dom_owner_body(dom), '.product-title h1 a, a[data-hook=product-link]') as `asin`,
    dom_first_href(dom, 'a.review-title-content, a[data-hook=review-title]') as `reviews_url`,
    dom_first_text(dom_owner_body(dom), '#filter-info-section div[data-hook=cr-filter-info-review-rating-count], #filter-info-section') as `ratingcount`,
    dom_first_href(dom, 'a[data-hook=format-strip]') as `sku_asin`,
    dom_attr(dom, 'id') as `comment_id`,
    dom_first_text(dom, '.review-date, span[data-hook=review-date]') as `comment_time`,
    dom_first_text(dom, 'a.a-profile[href~=profile] .a-profile-name') as `comment_name`,
    dom_first_text(dom, 'a.review-title-content, a[data-hook=review-title]') as `comment_title`,
    dom_first_href(dom, 'a.a-profile[href~=profile]') as `comment_name_url`,
    dom_first_text(dom, '.review-text-content, span[data-hook=review-body]') as `content`,
    str_first_float(dom_first_text(dom, 'a[title~=out of], i[data-hook=review-star-rating]'), 0.0) as `score`,
    cast(dom_img(dom_select_first(dom, 'div.review-image-tile-section')) as integer) as `ispic`,
    cast(dom_all_attrs(dom, 'div.review-image-tile-section img[data-hook=review-image-tile]', 'src') as varchar) as `pics`,
    str_first_integer(dom_first_text(dom, '.review-comments .cr-vote .cr-vote-text, span[data-hook=helpful-vote-statement]'), 0) as `helpfulnum`,
    format_timestamp(dom_attr(dom_select_first(dom, '#PulsarMetaInformation'), 'timestamp'), 'yyyy-MM-dd HH:mm:ss') as `task_time`,
    dom_own_texts(dom_select_first(dom, 'a[data-hook=format-strip]')) as `sku`
from load_and_select('{{url}}', '#cm_cr-review_list > div[data-hook=review]');
