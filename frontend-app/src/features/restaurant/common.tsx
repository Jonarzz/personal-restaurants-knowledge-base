import {Select} from 'antd';
import React from 'react';
import {Category} from '../../api';

export const prettifyCategoryName = (name: string) => {
  let output = name[0];
  for (let i = 1; i < name.length; i++) {
    let currentChar = name[i];
    if (currentChar === currentChar.toUpperCase()) {
      output += ' ' + currentChar.toLowerCase();
    } else {
      output += currentChar;
    }
  }
  return output;
};

export const CATEGORY_SELECT_OPTIONS = Object.entries(Category)
                                             .sort()
                                             .map(([name, value]) => (
                                               <Select.Option value={value} key={value}>
                                                 {prettifyCategoryName(name)}
                                               </Select.Option>
                                             ));

export const RATING_SELECT_OPTIONS = Array(10).fill(1)
                                              .map((x, y) => x + y)
                                              .map(value => (
                                                <Select.Option value={value} key={value}>
                                                  {value}
                                                </Select.Option>
                                              ));