select
    dom_all_texts(dom, 'tr#comparison_table_image_row > th a') as `Product name`,
    dom_all_texts(dom, 'tr#comparison_table_image_row > th a') as `Product image`,
    dom_all_texts(dom, 'tr#comparison_table_image_row > th a span:contains(Best Seller)') as `Label best seller`,
    dom_all_texts(dom, 'tr#comparison_custormer_rating_row > td') as `Customer Rating`,
    dom_all_texts(dom, 'tr#comparison_price_row > td') as `Price`,
    dom_all_texts(dom, 'tr#comparison_sold_by_row > td') as `Sold By`,
    dom_all_texts(dom, 'tr.comparison_other_attribute_row:contains(Color) > td') as `Color`,
    dom_all_texts(dom, 'tr.comparison_other_attribute_row:contains(Item Dimensions) > td') as `Item Dimensions`,
    dom_all_texts(dom, 'tr.comparison_other_attribute_row:contains(Material) > td') as `Material`
from dom_select(dom_load('{{url}}'), '#HLCXComparisonTable');
