import {Card} from 'antd';
import 'antd/dist/antd.css';
import React from 'react';

const App = () => (
  <>
    <Card size="small"
          title="Test card title"
          style={{width: 300}}
    >
      <p>Test card content</p>
    </Card>
  </>
);

export default App;
