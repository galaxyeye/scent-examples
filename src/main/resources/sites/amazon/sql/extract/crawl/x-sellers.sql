-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    str_substring_between(dom_base_uri(dom), 'seller=', '&') as `sellerID`,
    dom_first_text(dom, '#seller-summary h1#sellerName') as `seller_name`,
    dom_base_uri(dom) as `seller_url`,
    str_substring_between(dom_base_uri(dom), 'marketplaceID=', '&') as `marketplaceID`,
    dom_first_text(dom, '#seller-feedback-summary span a') as `feedbackSummary`,
    dom_all_texts(dom, '#feedback-content #feedback-summary-table tr:contains(Positive) td') as `highstarpercent`,
    dom_all_texts(dom, '#feedback-content #feedback-summary-table tr:contains(Neutral) td') as `middlestarpercent`,
    dom_all_texts(dom, '#feedback-content #feedback-summary-table tr:contains(Negative) td') as `badstarpercent`,
    dom_all_texts(dom, '#feedback-content #feedback-summary-table tr:contains(Count) td') as `feedback_num_12`,
    dom_all_texts(dom, '#feedback-content #feedback-summary-table tr:contains(Count) td') as `feedback_num`
from load_and_select(@url, ':root body');
