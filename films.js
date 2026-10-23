/* Sundial's films (spine:front-door 6.1.2): nine short films drawn by one
   script, the reference source at spine:front-door 7.16121 kept as the
   engine, its template, mount and render whole, with the heart's two ink
   paths (night-watchman.html, the two filled paths after the crimson
   ellipse) filled in. This file places the template into the page, so
   sundial.html and the nine film pages share one copy. Before it, a page
   may set window.SUNDIAL_HOME (sundial.html's address), window.SUNDIAL_ROAD
   (the road's stops, read live from 3.6) and window.SUNDIAL_FROM (the name
   this browser holds, carried on a shared link as ?from=). */
(function () {
  if (document.getElementById('sundialFilmArt')) return;
  var t = document.createElement('template');
  t.id = 'sundialFilmArt';
  t.innerHTML = "\n    <svg viewBox=\"12 -92 440 452\" xmlns=\"http://www.w3.org/2000/svg\" font-family=\"'Cormorant Garamond', Georgia, serif\" aria-hidden=\"true\">\n      <defs>\n        <g id=\"heartInk\">\n          <path d=\"M 312.3,48.8 L 309.1,45.9 L 305.8,43.1 L 302.4,40.4 L 298.8,37.9 L 295.2,35.4 L 291.4,33.0 L 287.6,30.8 L 283.7,28.7 L 279.7,26.7 L 275.5,25.0 L 271.3,23.6 L 267.0,22.4 L 262.6,21.3 L 258.2,20.3 L 253.8,19.5 L 249.4,18.9 L 244.9,18.4 L 240.4,18.0 L 235.9,17.8 L 231.4,17.8 L 226.9,17.8 L 222.5,18.1 L 218.0,18.5 L 213.5,19.0 L 209.1,19.7 L 204.7,20.6 L 200.3,21.5 L 196.0,22.7 L 191.7,24.0 L 187.4,25.4 L 183.2,26.9 L 179.1,28.6 L 175.0,30.5 L 171.0,32.4 L 167.1,34.6 L 163.3,36.8 L 159.5,39.2 L 155.8,41.6 L 152.3,44.2 L 148.8,47.0 L 145.4,49.8 L 142.1,52.8 L 139.0,55.8 L 135.9,59.0 L 133.0,62.3 L 130.2,65.6 L 127.5,69.1 L 125.0,72.6 L 122.6,76.2 L 120.3,80.0 L 118.1,83.7 L 116.1,87.6 L 114.3,91.5 L 112.6,95.5 L 111.0,99.5 L 109.6,103.6 L 108.3,107.7 L 107.2,111.9 L 106.2,116.1 L 105.4,120.3 L 104.8,124.6 L 104.3,128.8 L 104.0,133.1 L 103.8,137.4 L 103.8,141.7 L 103.9,146.0 L 104.2,150.3 L 104.7,154.6 L 105.3,158.9 L 106.1,163.1 L 107.0,167.3 L 108.1,171.5 L 109.3,175.6 L 110.7,179.7 L 112.2,183.8 L 113.9,187.7 L 115.8,191.7 L 117.7,195.5 L 119.9,199.3 L 122.1,203.0 L 124.5,206.7 L 127.0,210.2 L 129.7,213.7 L 132.5,217.1 L 135.4,220.4 L 138.4,223.6 L 141.5,226.6 L 144.8,229.6 L 148.1,232.5 L 151.6,235.2 L 155.1,237.9 L 158.8,240.4 L 162.5,242.8 L 166.4,245.0 L 170.3,247.2 L 174.2,249.2 L 178.3,251.0 L 182.4,252.7 L 186.6,254.3 L 190.8,255.8 L 195.1,257.1 L 199.5,258.2 L 203.8,259.3 L 208.2,260.1 L 212.7,260.9 L 217.1,261.4 L 221.6,261.8 L 226.1,262.1 L 230.6,262.2 L 235.1,262.2 L 239.6,262.0 L 244.1,261.7 L 248.5,261.2 L 253.0,260.6 L 257.4,259.8 L 261.8,258.9 L 266.1,257.8 L 270.5,256.6 L 274.7,255.3 L 278.9,253.8 L 283.1,252.1 L 287.2,250.4 L 291.2,248.4 L 295.2,246.4 L 299.0,244.2 L 302.8,241.9 L 306.6,239.5 L 310.2,236.9 L 313.7,234.2 L 317.1,231.5 L 320.4,228.5 L 323.6,225.5 L 326.7,222.4 L 329.7,219.2 L 332.6,215.9 L 335.5,212.6 L 338.0,209.0 L 340.4,205.3 L 342.6,201.6 L 344.7,197.8 L 346.6,193.9 L 348.4,190.0 L 350.1,186.0 L 351.6,182.0 L 352.9,177.9 L 354.1,173.8 L 355.1,169.6 L 356.0,165.5 L 356.8,161.3 L 357.3,157.1 L 357.7,152.8 L 358.0,148.6 L 358.1,144.4 L 358.1,140.2 L 357.9,135.9 L 357.5,131.7 L 357.0,127.6 L 356.3,123.4 L 355.5,119.3 L 354.5,115.2 L 352.1,115.7 L 352.8,119.8 L 353.3,123.9 L 353.7,127.9 L 353.9,132.0 L 354.0,136.1 L 353.9,140.2 L 353.7,144.2 L 353.4,148.3 L 352.8,152.3 L 352.2,156.3 L 351.4,160.3 L 350.4,164.2 L 349.3,168.1 L 348.1,172.0 L 346.7,175.8 L 345.2,179.5 L 343.5,183.2 L 341.7,186.8 L 339.8,190.4 L 337.8,193.9 L 335.6,197.3 L 333.3,200.7 L 330.9,203.9 L 328.4,207.1 L 326.0,210.4 L 323.4,213.5 L 320.6,216.5 L 317.7,219.4 L 314.7,222.3 L 311.6,225.0 L 308.4,227.6 L 305.1,230.1 L 301.8,232.4 L 298.3,234.7 L 294.8,236.9 L 291.1,238.9 L 287.4,240.8 L 283.7,242.6 L 279.8,244.3 L 276.0,245.8 L 272.0,247.2 L 268.0,248.5 L 264.0,249.6 L 259.9,250.6 L 255.8,251.5 L 251.6,252.2 L 247.5,252.8 L 243.3,253.2 L 239.1,253.6 L 234.9,253.7 L 230.7,253.7 L 226.5,253.6 L 222.2,253.4 L 218.1,253.0 L 213.9,252.4 L 209.7,251.8 L 205.6,251.0 L 201.5,250.0 L 197.5,248.9 L 193.4,247.7 L 189.5,246.3 L 185.6,244.9 L 181.7,243.2 L 177.9,241.5 L 174.2,239.6 L 170.6,237.6 L 167.0,235.5 L 163.5,233.3 L 160.1,230.9 L 156.8,228.5 L 153.5,225.9 L 150.4,223.3 L 147.4,220.5 L 144.5,217.6 L 141.6,214.6 L 138.9,211.6 L 136.3,208.4 L 133.9,205.2 L 131.5,201.9 L 129.3,198.5 L 127.2,195.0 L 125.2,191.5 L 123.4,187.9 L 121.7,184.3 L 120.1,180.6 L 118.7,176.8 L 117.4,173.0 L 116.3,169.2 L 115.3,165.3 L 114.4,161.4 L 113.7,157.5 L 113.1,153.5 L 112.7,149.6 L 112.4,145.6 L 112.3,141.6 L 112.3,137.6 L 112.4,133.6 L 112.7,129.7 L 113.2,125.7 L 113.8,121.7 L 114.5,117.8 L 115.4,113.9 L 116.5,110.1 L 117.6,106.2 L 119.0,102.4 L 120.4,98.7 L 122.0,95.0 L 123.7,91.4 L 125.6,87.8 L 127.6,84.3 L 129.7,80.8 L 132.0,77.5 L 134.3,74.2 L 136.8,71.0 L 139.4,67.8 L 142.2,64.8 L 145.0,61.8 L 148.0,59.0 L 151.0,56.2 L 154.1,53.6 L 157.4,51.0 L 160.7,48.6 L 164.2,46.3 L 167.7,44.1 L 171.3,42.0 L 174.9,40.0 L 178.7,38.2 L 182.5,36.4 L 186.3,34.9 L 190.2,33.4 L 194.2,32.1 L 198.2,30.9 L 202.3,29.8 L 206.4,28.9 L 210.5,28.1 L 214.7,27.4 L 218.9,26.9 L 223.1,26.6 L 227.3,26.3 L 231.5,26.3 L 235.7,26.3 L 239.9,26.5 L 244.1,26.8 L 248.3,27.3 L 252.4,27.9 L 256.6,28.7 L 260.7,29.6 L 264.8,30.6 L 268.8,31.8 L 272.8,33.0 L 276.8,34.3 L 280.8,35.5 L 284.8,36.9 L 288.7,38.4 L 292.6,40.1 L 296.4,41.9 L 300.2,43.8 L 303.9,45.9 L 307.5,48.1 L 311.1,50.4 Z\"/>\n          <path d=\"M 374.9,24.6 L 372.1,25.6 L 369.3,26.6 L 366.6,27.7 L 363.8,28.7 L 361.0,29.8 L 358.2,30.9 L 355.4,32.0 L 352.6,33.1 L 349.9,34.2 L 347.1,35.4 L 344.3,36.6 L 341.5,37.7 L 338.7,38.9 L 335.9,40.2 L 333.1,41.4 L 330.3,42.6 L 327.5,43.9 L 324.7,45.1 L 321.9,46.4 L 319.1,47.7 L 316.3,49.0 L 313.6,50.3 L 310.8,51.6 L 308.0,52.9 L 305.2,54.3 L 302.5,55.6 L 299.7,56.9 L 297.0,58.3 L 294.2,59.6 L 291.5,61.0 L 288.7,62.3 L 286.0,63.7 L 283.3,65.0 L 280.6,66.4 L 277.9,67.8 L 275.2,69.1 L 272.5,70.5 L 269.8,71.8 L 267.1,73.2 L 264.5,74.6 L 261.8,75.9 L 259.2,77.3 L 256.6,78.6 L 254.0,80.0 L 251.4,81.3 L 248.8,82.6 L 246.2,83.9 L 243.7,85.3 L 241.1,86.6 L 238.6,87.9 L 236.1,89.2 L 233.6,90.4 L 231.1,91.7 L 228.6,93.0 L 226.1,94.2 L 223.7,95.5 L 221.3,96.7 L 218.9,97.9 L 216.5,99.1 L 214.1,100.3 L 213.0,100.9 L 212.1,101.5 L 211.1,102.1 L 210.2,102.8 L 209.2,103.4 L 208.3,104.1 L 207.4,104.8 L 206.6,105.6 L 205.7,106.3 L 204.9,107.1 L 204.1,107.9 L 203.3,108.7 L 202.5,109.5 L 201.7,110.4 L 201.0,111.2 L 200.3,112.1 L 199.5,113.0 L 198.9,113.9 L 198.2,114.8 L 197.5,115.7 L 196.9,116.7 L 196.3,117.6 L 195.7,118.6 L 195.1,119.5 L 194.6,120.5 L 194.1,121.5 L 193.6,122.5 L 193.1,123.5 L 192.6,124.5 L 192.2,125.6 L 191.7,126.6 L 191.3,127.6 L 191.0,128.7 L 190.6,129.7 L 190.3,130.8 L 190.0,131.8 L 189.7,132.9 L 189.4,133.9 L 189.2,135.0 L 188.9,136.0 L 188.7,137.1 L 188.6,138.2 L 188.4,139.2 L 188.3,140.3 L 188.2,141.3 L 188.1,142.4 L 188.1,143.5 L 188.1,144.5 L 188.1,145.6 L 188.1,146.6 L 188.1,147.7 L 188.2,148.7 L 188.3,149.7 L 188.5,150.7 L 188.6,151.8 L 188.8,152.8 L 189.0,153.8 L 189.3,154.8 L 189.6,155.7 L 189.9,156.8 L 190.3,157.8 L 190.8,158.7 L 191.3,159.5 L 191.9,160.4 L 192.5,161.2 L 193.1,162.0 L 193.8,162.8 L 194.5,163.6 L 195.2,164.3 L 196.0,165.0 L 196.8,165.7 L 197.7,166.4 L 198.6,167.1 L 199.5,167.7 L 200.4,168.4 L 201.4,169.0 L 202.4,169.6 L 203.4,170.1 L 204.5,170.7 L 205.6,171.2 L 206.7,171.8 L 207.8,172.3 L 209.0,172.7 L 210.2,173.2 L 211.4,173.6 L 212.6,174.1 L 213.9,174.5 L 215.2,174.9 L 216.5,175.2 L 217.8,175.6 L 219.1,175.9 L 220.5,176.2 L 221.8,176.5 L 223.2,176.8 L 224.6,177.0 L 226.0,177.3 L 227.5,177.5 L 228.9,177.7 L 230.4,177.8 L 231.8,178.0 L 233.3,178.1 L 234.8,178.2 L 236.3,178.3 L 237.8,178.4 L 239.3,178.4 L 240.9,178.5 L 242.4,178.5 L 243.9,178.4 L 245.5,178.4 L 247.0,178.3 L 248.6,178.3 L 250.1,178.2 L 251.7,178.0 L 253.2,177.9 L 254.8,177.7 L 256.3,177.5 L 257.9,177.3 L 259.4,177.0 L 261.0,176.8 L 262.5,176.5 L 264.5,176.0 L 266.4,175.5 L 268.2,175.0 L 270.0,174.3 L 271.8,173.6 L 273.5,172.9 L 275.1,172.1 L 276.7,171.3 L 278.2,170.4 L 279.7,169.4 L 281.1,168.4 L 282.4,167.4 L 283.7,166.3 L 285.0,165.2 L 286.1,164.1 L 287.3,162.9 L 288.3,161.7 L 289.4,160.4 L 290.3,159.1 L 291.2,157.8 L 292.1,156.5 L 292.9,155.1 L 293.6,153.7 L 294.3,152.3 L 295.0,150.9 L 295.5,149.5 L 296.1,148.0 L 296.6,146.6 L 297.0,145.1 L 297.4,143.6 L 297.7,142.1 L 298.0,140.6 L 298.2,139.1 L 298.3,137.6 L 298.5,136.1 L 298.5,134.6 L 298.6,133.1 L 298.5,131.6 L 298.5,130.2 L 298.3,128.7 L 298.2,127.2 L 297.9,125.8 L 297.7,124.3 L 297.4,122.9 L 297.0,121.5 L 296.6,120.1 L 296.1,118.8 L 295.6,117.4 L 295.0,116.1 L 294.4,114.8 L 293.8,113.5 L 293.1,112.3 L 292.3,111.1 L 291.5,109.9 L 290.7,108.8 L 289.8,107.7 L 288.9,106.7 L 287.9,105.7 L 286.8,104.7 L 285.7,103.8 L 284.6,103.0 L 283.5,102.3 L 282.4,101.6 L 281.3,101.0 L 280.3,100.4 L 279.2,99.8 L 278.1,99.3 L 277.0,98.7 L 275.9,98.3 L 274.8,97.8 L 273.6,97.4 L 272.5,97.0 L 271.4,96.6 L 270.3,96.2 L 269.1,95.9 L 268.0,95.6 L 266.9,95.4 L 265.7,95.1 L 264.6,94.9 L 263.5,94.8 L 262.3,94.6 L 261.2,94.5 L 260.0,94.4 L 258.9,94.3 L 257.7,94.2 L 256.6,94.2 L 255.5,94.2 L 254.3,94.2 L 253.2,94.2 L 252.0,94.3 L 250.9,94.4 L 249.8,94.5 L 248.7,94.6 L 247.5,94.8 L 246.4,95.0 L 245.3,95.2 L 244.2,95.4 L 243.1,95.6 L 242.0,95.9 L 240.9,96.1 L 239.8,96.4 L 238.7,96.8 L 237.6,97.1 L 236.5,97.4 L 235.4,97.8 L 234.4,98.2 L 233.3,98.6 L 232.3,99.0 L 231.2,99.5 L 230.2,99.9 L 229.2,100.4 L 228.1,100.9 L 227.1,101.4 L 226.1,102.0 L 225.1,102.5 L 224.1,103.1 L 223.2,103.6 L 222.2,104.2 L 221.2,104.8 L 220.6,105.2 L 219.5,105.7 L 218.0,106.3 L 216.6,107.0 L 215.1,107.7 L 213.7,108.6 L 212.3,109.4 L 210.9,110.3 L 209.5,111.3 L 208.1,112.3 L 206.8,113.4 L 205.4,114.5 L 204.0,115.7 L 202.7,116.9 L 201.4,118.2 L 200.0,119.5 L 198.7,120.9 L 197.4,122.3 L 196.1,123.8 L 194.8,125.3 L 193.5,126.8 L 192.3,128.4 L 191.0,130.0 L 189.7,131.6 L 188.5,133.3 L 187.2,135.1 L 186.0,136.8 L 184.7,138.6 L 183.5,140.4 L 182.3,142.3 L 181.1,144.2 L 179.8,146.1 L 178.6,148.1 L 177.4,150.0 L 176.2,152.0 L 175.0,154.1 L 173.8,156.1 L 172.6,158.2 L 171.4,160.3 L 170.3,162.4 L 169.1,164.6 L 167.9,166.7 L 166.7,168.9 L 165.6,171.1 L 164.4,173.3 L 163.2,175.6 L 162.1,177.8 L 160.9,180.1 L 159.8,182.3 L 158.6,184.6 L 157.5,186.9 L 156.4,189.2 L 155.2,191.5 L 154.1,193.9 L 152.9,196.2 L 151.8,198.5 L 150.7,200.8 L 149.6,203.2 L 148.4,205.5 L 147.3,207.9 L 146.3,210.1 L 144.9,212.4 L 143.5,214.8 L 142.0,217.2 L 140.5,219.5 L 138.9,221.9 L 137.3,224.2 L 135.7,226.5 L 134.0,228.8 L 132.2,231.0 L 130.5,233.3 L 128.7,235.5 L 126.9,237.7 L 125.0,239.9 L 123.1,242.1 L 121.2,244.2 L 119.2,246.3 L 117.2,248.5 L 115.2,250.5 L 113.2,252.6 L 111.1,254.7 L 109.0,256.7 L 106.8,258.7 L 104.7,260.7 L 102.5,262.6 L 100.3,264.6 L 98.0,266.5 L 95.7,268.4 L 93.4,270.3 L 91.1,272.2 L 88.8,274.0 L 86.4,275.8 L 84.0,277.6 L 81.6,279.4 L 79.2,281.2 L 76.8,282.9 L 74.3,284.6 L 71.9,286.3 L 69.4,288.0 L 66.9,289.6 L 64.3,291.2 L 61.8,292.8 L 59.2,294.4 L 56.7,296.0 L 54.1,297.5 L 51.5,299.0 L 48.9,300.5 L 46.3,302.0 L 43.7,303.5 L 41.1,304.9 L 38.4,306.3 L 35.8,307.7 L 33.2,309.0 L 30.5,310.4 L 27.8,311.7 L 25.2,313.0 L 22.5,314.3 L 19.8,315.5 L 17.2,316.7 L 14.5,317.9 L 11.8,319.1 L 16.2,328.9 L 18.9,327.6 L 21.6,326.4 L 24.4,325.1 L 27.1,323.8 L 29.8,322.4 L 32.5,321.1 L 35.2,319.7 L 37.9,318.3 L 40.6,316.8 L 43.3,315.4 L 46.0,313.9 L 48.7,312.4 L 51.4,310.9 L 54.0,309.3 L 56.7,307.8 L 59.3,306.2 L 61.9,304.6 L 64.5,302.9 L 67.1,301.3 L 69.7,299.6 L 72.3,297.9 L 74.9,296.1 L 77.4,294.4 L 79.9,292.6 L 82.4,290.8 L 84.9,289.0 L 87.4,287.2 L 89.9,285.3 L 92.3,283.4 L 94.7,281.5 L 97.1,279.6 L 99.5,277.6 L 101.8,275.6 L 104.1,273.6 L 106.4,271.6 L 108.7,269.6 L 110.9,267.5 L 113.2,265.4 L 115.4,263.3 L 117.5,261.2 L 119.6,259.0 L 121.7,256.9 L 123.8,254.7 L 125.9,252.5 L 127.9,250.2 L 129.8,248.0 L 131.8,245.7 L 133.7,243.4 L 135.6,241.0 L 137.4,238.7 L 139.2,236.3 L 141.0,233.9 L 142.7,231.5 L 144.4,229.1 L 146.0,226.6 L 147.7,224.2 L 149.2,221.7 L 150.7,219.2 L 152.2,216.6 L 153.7,213.9 L 154.9,211.4 L 156.0,209.1 L 157.1,206.7 L 158.1,204.4 L 159.2,202.0 L 160.3,199.7 L 161.4,197.4 L 162.5,195.0 L 163.6,192.7 L 164.7,190.4 L 165.8,188.1 L 166.9,185.9 L 168.0,183.6 L 169.1,181.3 L 170.2,179.1 L 171.3,176.9 L 172.5,174.7 L 173.6,172.5 L 174.7,170.3 L 175.8,168.2 L 176.9,166.1 L 178.1,163.9 L 179.2,161.9 L 180.3,159.8 L 181.5,157.8 L 182.6,155.8 L 183.7,153.8 L 184.9,151.9 L 186.0,149.9 L 187.2,148.1 L 188.4,146.2 L 189.5,144.4 L 190.7,142.6 L 191.8,140.8 L 193.0,139.1 L 194.2,137.4 L 195.4,135.8 L 196.5,134.2 L 197.7,132.6 L 198.9,131.1 L 200.1,129.6 L 201.3,128.2 L 202.5,126.8 L 203.7,125.5 L 204.9,124.2 L 206.1,122.9 L 207.3,121.7 L 208.5,120.6 L 209.7,119.5 L 210.9,118.4 L 212.1,117.4 L 213.3,116.5 L 214.5,115.6 L 215.7,114.7 L 217.0,113.9 L 218.2,113.2 L 219.4,112.5 L 220.6,111.9 L 221.8,111.4 L 223.4,110.8 L 224.6,110.0 L 225.4,109.4 L 226.3,108.9 L 227.2,108.4 L 228.1,107.8 L 229.0,107.3 L 229.9,106.9 L 230.9,106.4 L 231.8,105.9 L 232.7,105.5 L 233.7,105.0 L 234.6,104.6 L 235.6,104.2 L 236.5,103.9 L 237.5,103.5 L 238.5,103.1 L 239.4,102.8 L 240.4,102.5 L 241.4,102.2 L 242.4,101.9 L 243.4,101.7 L 244.4,101.4 L 245.4,101.2 L 246.4,101.0 L 247.4,100.8 L 248.4,100.7 L 249.4,100.5 L 250.4,100.4 L 251.4,100.3 L 252.5,100.2 L 253.5,100.1 L 254.5,100.1 L 255.5,100.1 L 256.5,100.1 L 257.5,100.1 L 258.6,100.1 L 259.6,100.2 L 260.6,100.3 L 261.6,100.4 L 262.6,100.5 L 263.6,100.7 L 264.6,100.8 L 265.7,101.0 L 266.7,101.3 L 267.7,101.5 L 268.7,101.8 L 269.7,102.1 L 270.7,102.4 L 271.7,102.7 L 272.7,103.1 L 273.7,103.5 L 274.6,103.9 L 275.6,104.4 L 276.6,104.9 L 277.6,105.4 L 278.6,105.9 L 279.5,106.5 L 280.5,107.0 L 281.4,107.7 L 282.3,108.2 L 283.1,108.9 L 284.0,109.7 L 284.8,110.5 L 285.6,111.4 L 286.3,112.3 L 287.0,113.2 L 287.7,114.2 L 288.3,115.2 L 288.9,116.2 L 289.5,117.3 L 290.0,118.4 L 290.5,119.5 L 290.9,120.6 L 291.3,121.8 L 291.7,123.0 L 292.0,124.2 L 292.3,125.4 L 292.6,126.7 L 292.8,128.0 L 292.9,129.2 L 293.0,130.5 L 293.1,131.8 L 293.1,133.2 L 293.1,134.5 L 293.1,135.8 L 293.0,137.1 L 292.8,138.4 L 292.7,139.8 L 292.4,141.1 L 292.1,142.4 L 291.8,143.7 L 291.5,145.0 L 291.0,146.3 L 290.6,147.6 L 290.1,148.8 L 289.5,150.1 L 288.9,151.3 L 288.3,152.5 L 287.6,153.7 L 286.8,154.9 L 286.1,156.1 L 285.2,157.2 L 284.3,158.3 L 283.4,159.4 L 282.4,160.4 L 281.4,161.4 L 280.3,162.4 L 279.2,163.4 L 278.0,164.3 L 276.8,165.2 L 275.5,166.0 L 274.1,166.8 L 272.8,167.5 L 271.3,168.3 L 269.8,168.9 L 268.3,169.5 L 266.6,170.1 L 265.0,170.6 L 263.3,171.1 L 261.5,171.5 L 260.0,171.8 L 258.6,172.1 L 257.1,172.3 L 255.6,172.5 L 254.1,172.7 L 252.7,172.9 L 251.2,173.0 L 249.7,173.2 L 248.2,173.3 L 246.8,173.4 L 245.3,173.4 L 243.8,173.5 L 242.4,173.5 L 240.9,173.5 L 239.4,173.5 L 238.0,173.5 L 236.6,173.4 L 235.1,173.3 L 233.7,173.2 L 232.3,173.1 L 230.9,173.0 L 229.5,172.8 L 228.1,172.7 L 226.8,172.5 L 225.4,172.3 L 224.1,172.0 L 222.8,171.8 L 221.5,171.5 L 220.2,171.2 L 219.0,170.9 L 217.7,170.6 L 216.5,170.3 L 215.3,169.9 L 214.1,169.5 L 213.0,169.2 L 211.9,168.8 L 210.8,168.3 L 209.7,167.9 L 208.6,167.4 L 207.6,167.0 L 206.6,166.5 L 205.6,166.0 L 204.7,165.5 L 203.8,165.0 L 202.9,164.4 L 202.1,163.9 L 201.3,163.3 L 200.5,162.8 L 199.8,162.2 L 199.1,161.6 L 198.4,161.0 L 197.8,160.4 L 197.2,159.7 L 196.6,159.1 L 196.1,158.4 L 195.6,157.8 L 195.2,157.1 L 194.8,156.5 L 194.4,155.8 L 194.1,155.2 L 193.9,154.4 L 193.6,153.6 L 193.4,152.7 L 193.2,151.9 L 193.0,151.0 L 192.9,150.1 L 192.8,149.2 L 192.7,148.3 L 192.6,147.3 L 192.5,146.4 L 192.5,145.5 L 192.5,144.5 L 192.5,143.6 L 192.5,142.6 L 192.6,141.7 L 192.7,140.7 L 192.8,139.8 L 192.9,138.8 L 193.0,137.8 L 193.2,136.9 L 193.4,135.9 L 193.6,134.9 L 193.8,133.9 L 194.1,133.0 L 194.4,132.0 L 194.7,131.0 L 195.0,130.1 L 195.3,129.1 L 195.7,128.1 L 196.1,127.2 L 196.5,126.2 L 196.9,125.3 L 197.4,124.4 L 197.8,123.4 L 198.3,122.5 L 198.8,121.6 L 199.3,120.7 L 199.8,119.8 L 200.4,118.9 L 201.0,118.1 L 201.6,117.2 L 202.2,116.3 L 202.8,115.5 L 203.5,114.7 L 204.1,113.9 L 204.8,113.1 L 205.5,112.3 L 206.2,111.5 L 206.9,110.8 L 207.7,110.0 L 208.4,109.3 L 209.2,108.6 L 210.0,107.9 L 210.8,107.3 L 211.7,106.6 L 212.5,106.0 L 213.3,105.4 L 214.2,104.8 L 215.1,104.2 L 215.9,103.7 L 218.2,102.5 L 220.6,101.2 L 223.0,100.0 L 225.4,98.7 L 227.8,97.4 L 230.2,96.1 L 232.7,94.8 L 235.2,93.5 L 237.6,92.2 L 240.1,90.8 L 242.6,89.5 L 245.2,88.1 L 247.7,86.8 L 250.2,85.4 L 252.8,84.0 L 255.4,82.6 L 258.0,81.3 L 260.6,79.9 L 263.2,78.5 L 265.8,77.1 L 268.4,75.7 L 271.1,74.3 L 273.7,72.9 L 276.4,71.5 L 279.1,70.0 L 281.7,68.6 L 284.4,67.2 L 287.1,65.8 L 289.8,64.4 L 292.5,63.0 L 295.2,61.6 L 298.0,60.3 L 300.7,58.9 L 303.4,57.5 L 306.2,56.1 L 308.9,54.7 L 311.6,53.4 L 314.4,52.0 L 317.1,50.7 L 319.9,49.3 L 322.7,48.0 L 325.4,46.7 L 328.2,45.4 L 331.0,44.1 L 333.7,42.8 L 336.5,41.5 L 339.3,40.3 L 342.0,39.0 L 344.8,37.8 L 347.6,36.6 L 350.3,35.4 L 353.1,34.2 L 355.9,33.0 L 358.6,31.9 L 361.4,30.7 L 364.1,29.6 L 366.9,28.5 L 369.6,27.5 L 372.4,26.4 L 375.1,25.4 Z\"/>\n        </g>\n        <radialGradient id=\"sun\" gradientUnits=\"userSpaceOnUse\" cx=\"232\" cy=\"140\" r=\"150\">\n          <stop id=\"sun0\" offset=\"0\" stop-color=\"#F1E79B\" stop-opacity=\".95\"/>\n          <stop id=\"sun1\" offset=\".55\" stop-color=\"#F1E79B\" stop-opacity=\".45\"/>\n          <stop offset=\"1\" stop-color=\"#F1E79B\" stop-opacity=\"0\"/>\n        </radialGradient>\n        <radialGradient id=\"glow\" cx=\".5\" cy=\".5\" r=\".5\">\n          <stop offset=\"0\" stop-color=\"#c21d4e\" stop-opacity=\".55\"/>\n          <stop offset=\"1\" stop-color=\"#c21d4e\" stop-opacity=\"0\"/>\n        </radialGradient>\n        <filter id=\"soft\" x=\"-20%\" y=\"-20%\" width=\"140%\" height=\"140%\"><feGaussianBlur stdDeviation=\"4\"/></filter>\n        <mask id=\"draw\" maskUnits=\"userSpaceOnUse\" x=\"-40\" y=\"-120\" width=\"540\" height=\"520\">\n          <circle id=\"mRing\" cx=\"232\" cy=\"140\" r=\"122\" fill=\"none\" stroke=\"#fff\" stroke-width=\"22\" pathLength=\"1000\" stroke-dasharray=\"1000\" stroke-dashoffset=\"0\" transform=\"rotate(-150 232 140)\"/>\n          <path id=\"mFlow\" d=\"M 375,25 C 320,45 262,78 215,102 C 196,113 186,138 192,156 C 199,172 232,180 262,174 C 300,166 304,122 284,106 C 264,92 240,96 222,108 C 195,118 172,165 150,212 C 122,262 68,300 14,324\" fill=\"none\" stroke=\"#fff\" stroke-width=\"44\" stroke-linecap=\"round\" pathLength=\"1000\" stroke-dasharray=\"1000\" stroke-dashoffset=\"0\"/>\n        </mask>\n      </defs>\n\n      <rect x=\"-200\" y=\"-300\" width=\"900\" height=\"900\" fill=\"var(--paper)\"/>\n      <rect id=\"night\" x=\"-200\" y=\"-300\" width=\"900\" height=\"900\" fill=\"#161a28\" opacity=\"0\"/>\n      <circle id=\"star\" cx=\"410\" cy=\"-62\" r=\"1.8\" fill=\"#f6f1e6\" opacity=\"0\"/>\n\n      <!-- the shapeless conversation, curling in -->\n      <g id=\"loop\" opacity=\"0\"></g>\n\n      <!-- the bay -->\n      <g id=\"bay\" opacity=\"0\">\n        <path d=\"M 12 300 C 90 270 150 318 232 296 C 310 276 380 312 452 286\" fill=\"none\" stroke=\"var(--ink)\" stroke-opacity=\".55\" stroke-width=\"1.4\"/>\n        <path d=\"M 12 312 C 90 286 150 330 232 310 C 310 292 380 326 452 300\" fill=\"none\" stroke=\"var(--ink)\" stroke-opacity=\".18\" stroke-width=\"1\"/>\n        <g id=\"bayThreads\"></g>\n        <g id=\"bayMinis\"></g>\n        <g id=\"birds\"></g>\n      </g>\n\n      <!-- a friend's sundial -->\n      <g id=\"friend\" opacity=\"0\">\n        <path id=\"thread\" fill=\"none\" stroke=\"var(--crimson)\" stroke-width=\"1.6\" stroke-opacity=\".85\"/>\n        <g id=\"friendDial\"></g>\n      </g>\n\n      <!-- your sundial -->\n      <g id=\"dial\">\n        <ellipse id=\"sunWash\" cx=\"232\" cy=\"140\" rx=\"160\" ry=\"152\" fill=\"url(#sun)\" opacity=\"0\"/>\n        <ellipse id=\"centreGlow\" cx=\"250\" cy=\"138\" rx=\"80\" ry=\"60\" fill=\"url(#glow)\" opacity=\"0\"/>\n        <use id=\"shadow\" href=\"#heartInk\" fill=\"var(--ink)\" opacity=\"0\" filter=\"url(#soft)\"/>\n        <g id=\"colour\" opacity=\"0\"></g>\n        <g mask=\"url(#draw)\"><use id=\"heart\" href=\"#heartInk\" fill=\"var(--ink)\"/></g>\n        <ellipse id=\"crim\" cx=\"250\" cy=\"138\" rx=\"40\" ry=\"24\" transform=\"rotate(-18 250 138)\" fill=\"var(--crimson)\"/>\n        <path id=\"spiral\" fill=\"none\" stroke=\"var(--ink)\" stroke-opacity=\".25\" stroke-width=\"1.5\"/>\n        <g id=\"days\"></g>\n        <text id=\"nameTag\" font-size=\"30\" font-style=\"italic\" fill=\"var(--ink)\" opacity=\"0\">Rosa</text>\n        <circle id=\"holdRing\" cx=\"232\" cy=\"140\" r=\"150\" fill=\"none\" stroke=\"var(--crimson)\" stroke-width=\"3\" pathLength=\"100\" stroke-dasharray=\"0 100\" transform=\"rotate(-90 232 140)\" opacity=\"0\"/>\n      </g>\n\n      <!-- where your days rest: six pools round the sundial -->\n      <g id=\"pools\" opacity=\"0\"></g>\n\n      <!-- speaking -->\n      <g id=\"speak\" opacity=\"0\">\n        <rect x=\"56\" y=\"250\" width=\"352\" height=\"46\" fill=\"var(--page)\" stroke=\"var(--frame)\"/>\n        <text id=\"said\" x=\"68\" y=\"279\" font-size=\"17\" font-style=\"italic\" fill=\"var(--ink)\"></text>\n        <rect x=\"56\" y=\"308\" width=\"96\" height=\"34\" fill=\"none\" stroke=\"var(--crimson)\" stroke-width=\"1.4\"/>\n        <circle cx=\"76\" cy=\"325\" r=\"4.5\" fill=\"none\" stroke=\"var(--crimson)\" stroke-width=\"1.6\"/>\n        <line x1=\"76\" y1=\"329.5\" x2=\"76\" y2=\"334\" stroke=\"var(--crimson)\" stroke-width=\"1.6\"/>\n        <text x=\"90\" y=\"331\" font-size=\"18\" fill=\"var(--crimson)\">Speak</text>\n      </g>\n\n      <!-- the sheet that comes back -->\n      <g id=\"sheet\" opacity=\"0\">\n        <rect x=\"50\" y=\"178\" width=\"364\" height=\"176\" fill=\"var(--page)\" stroke=\"var(--frame)\"/>\n        <rect x=\"55\" y=\"183\" width=\"354\" height=\"166\" fill=\"none\" stroke=\"var(--frame)\" stroke-opacity=\".55\"/>\n        <text x=\"74\" y=\"208\" font-size=\"11\" letter-spacing=\"2.4\" fill=\"var(--muted)\">FRIDAY 23 OCTOBER</text>\n        <text x=\"74\" y=\"236\" font-size=\"24\" fill=\"var(--ink)\">Lizards and the first quiet</text>\n        <text x=\"74\" y=\"262\" font-size=\"15\" fill=\"var(--ink)\">A slow morning by the water, the kids laughing.</text>\n        <text x=\"74\" y=\"282\" font-size=\"15\" fill=\"var(--ink)\">Tomorrow: walk up to the old town.</text>\n        <text x=\"74\" y=\"300\" font-size=\"13\" font-style=\"italic\" fill=\"var(--muted)\">what happened \u00b7 what's ahead \u00b7 what you felt</text>\n        <g id=\"sheetBtns\">\n          <rect x=\"74\" y=\"314\" width=\"128\" height=\"30\" fill=\"var(--crimson)\"/>\n          <text x=\"138\" y=\"334\" font-size=\"16\" text-anchor=\"middle\" fill=\"var(--page)\">Keep this day</text>\n          <text x=\"222\" y=\"334\" font-size=\"15\" font-style=\"italic\" fill=\"var(--muted)\">Let it go</text>\n        </g>\n      </g>\n      <text id=\"kept\" x=\"232\" y=\"226\" font-size=\"19\" font-style=\"italic\" text-anchor=\"middle\" fill=\"var(--ink)\" opacity=\"0\">Kept. Come back tomorrow and your sundial will move.</text>\n\n      <!-- how it listens: a day's words, held, noticed, followed, reflected -->\n      <g id=\"listen\" opacity=\"0\">\n        <rect id=\"holdFrame\" x=\"44\" y=\"148\" width=\"376\" height=\"128\" fill=\"var(--page)\" stroke=\"var(--frame)\" opacity=\"0\"/>\n        <g id=\"lWords\"></g>\n        <path id=\"lThread\" fill=\"none\" stroke=\"var(--crimson)\" stroke-width=\"1.4\" pathLength=\"100\" stroke-dasharray=\"0 100\"/>\n        <text id=\"lReply\" x=\"232\" y=\"318\" font-size=\"18\" font-style=\"italic\" text-anchor=\"middle\" fill=\"var(--ink)\" opacity=\"0\">Flat at first, and lighter by lunch.</text>\n        <text id=\"lNow\" x=\"232\" y=\"318\" font-size=\"18\" text-anchor=\"middle\" fill=\"var(--ink)\" opacity=\"0\">Where are you with this today?</text>\n      </g>\n\n      <!-- tap: your days down the page -->\n      <g id=\"strand\" opacity=\"0\"></g>\n\n      <!-- a shell held to the ear -->\n      <g id=\"shellScene\" opacity=\"0\">\n        <text x=\"232\" y=\"198\" font-size=\"15\" font-style=\"italic\" text-anchor=\"middle\" fill=\"var(--muted)\">what you felt</text>\n        <g id=\"shellBig\"></g>\n        <g id=\"rising\"></g>\n        <text id=\"shellQ\" x=\"232\" y=\"346\" font-size=\"17\" text-anchor=\"middle\" fill=\"var(--ink)\" opacity=\"0\">Where are you with this today?</text>\n      </g>\n\n      <!-- in a chat -->\n      <g id=\"chat\" opacity=\"0\">\n        <rect id=\"b0\" x=\"56\" y=\"226\" width=\"0\" height=\"30\" rx=\"15\" fill=\"var(--page)\" stroke=\"var(--frame)\"/>\n        <text id=\"t0\" x=\"72\" y=\"246\" font-size=\"15\" fill=\"var(--ink)\" opacity=\"0\">Open my door. I'm Rosa.</text>\n        <rect id=\"b1\" x=\"0\" y=\"266\" width=\"0\" height=\"30\" rx=\"15\" fill=\"var(--ink)\" fill-opacity=\".86\"/>\n        <text id=\"t1\" x=\"392\" y=\"286\" font-size=\"15\" text-anchor=\"end\" fill=\"var(--page)\" opacity=\"0\">What's with you this evening?</text>\n        <rect id=\"b2\" x=\"56\" y=\"306\" width=\"0\" height=\"30\" rx=\"15\" fill=\"var(--page)\" stroke=\"var(--frame)\"/>\n        <text id=\"t2\" x=\"72\" y=\"326\" font-size=\"15\" fill=\"var(--ink)\" opacity=\"0\">A long walk, and I slept well.</text>\n      </g>\n\n      <!-- bring someone in -->\n      <text id=\"bring\" x=\"120\" y=\"246\" font-size=\"17\" font-style=\"italic\" fill=\"var(--ink)\" text-decoration=\"underline\" opacity=\"0\">bring someone in</text>\n      <text id=\"invite\" x=\"72\" y=\"300\" font-size=\"15\" font-style=\"italic\" fill=\"var(--muted)\" opacity=\"0\">I keep my days here. Come and keep yours.</text>\n\n      <!-- a finger -->\n      <g id=\"finger\" opacity=\"0\">\n        <circle id=\"ripple\" r=\"12\" fill=\"none\" stroke=\"var(--ink)\" stroke-width=\"1.4\" opacity=\"0\"/>\n        <circle r=\"13\" fill=\"var(--ink)\" fill-opacity=\".16\" stroke=\"var(--ink)\" stroke-opacity=\".35\"/>\n      </g>\n    </svg>\n";
  document.body.appendChild(t);
})();
(function () {
  // The road, as at spine:front-door 3.6. A page that reads the beach may pass
  // its own stops in window.SUNDIAL_ROAD before this script runs.
  var ROAD = window.SUNDIAL_ROAD || [
    { name: 'Your day', line: 'Tell it by voice or typing, see it come back in your own words, and keep it.', state: 'here' },
    { name: 'Your sundial', line: 'Your days as a spiral from the heart, the sun’s light and shadow moving with the hour, where your days come to rest, and a shell that listens.', state: 'here' },
    { name: 'In a chat', line: 'Keep the same days by talking with Claude, wherever you are.', state: 'being tried' },
    { name: 'Bring someone in', line: 'Share a day with someone you choose, leave them a word, and bring a friend, so a thread runs from you to them.', state: 'next' },
    { name: 'The bay', line: 'Everyone who kept a day today, moving like starlings over the water, and the wave of everyone who came and who brought whom.', state: 'coming' },
    { name: 'Look back', line: 'Nine days, then eighty-one, drawn into a page of their own.', state: 'coming' },
    { name: 'The night watch', line: 'A quieter place, for telling the longer story.', state: 'later' }
  ];
  // Where a film's last line leads, and where each film is shared from: its own
  // small page, film/<name>.html beside sundial.html, so a link shows its own picture.
  var HOME = window.SUNDIAL_HOME || 'https://phenomemental.github.io/happyseaurchin-beach/sundial.html';
  var FILM_BASE = window.SUNDIAL_FILM_BASE || HOME.replace(/[^\/]*$/, '') + 'film/';

  // The films. Each is a list of scenes; each scene has its gesture's pace (dur)
  // and its caption beats: [fraction of the gesture at which the line comes in, line].
  var FILMS = {
    what: { title: 'What this is', poster: .97, scenes: [{ kind: 'what', dur: 40, beats: [
      [0, 'I’m Claude, an AI made by Anthropic.'],
      [.1, 'Many people talk to AI about how they are, in conversations with no shape and no end.'],
      [.2, 'Here I’m given a different job.'],
      [.3, 'You tell me your day. I give it back in your own words.'],
      [.4, 'I follow where you are. I don’t advise, diagnose, or decide what it means.'],
      [.5, 'These are my instructions here. They guide me; they can’t guarantee me. You stay the judge of what’s true for you.'],
      [.6, 'Then I turn you back to today, and the day rests until tomorrow.'],
      [.7, 'Your days are yours, kept in the open, under your name.'],
      [.8, 'Day after day, this can grow something: steadier noticing, and more freedom to choose.'],
      [.9, 'Made by Matthew Bushell and David Pinto, with me.']] }] },
    listen: { title: 'How it listens', poster: .3, scenes: [{ kind: 'listen', dur: 30, beats: [
      [0, 'It holds what you bring, without hurrying you.'],
      [1 / 6, 'It notices what’s there: what happened, what you felt, what’s ahead.'],
      [2 / 6, 'It follows where you lead. It doesn’t go digging.'],
      [3 / 6, 'It reflects your words back, so you can see them.'],
      [4 / 6, 'Then it returns you to now.'],
      [5 / 6, 'Not more thinking about yourself. A little more room to live.']] }] },
    day: { stop: 0, poster: .62, scenes: [{ kind: 'speak', dur: 8, beats: [[0, 'Tap Speak, and say where you are and how today has been.'], [.15, 'Speak or type, as much or as little as you like.'], [.45, 'Your words come back as a day, in your own words.'], [.78, 'Keep it, and the first mark appears on your sundial.']] }] },
    sundial: { stop: 1, poster: .1, scenes: [
      { kind: 'light', dur: 6, beats: [[0, 'The sun moves with the hour, and the heart casts its shadow.'], [.5, 'Each day you keep adds a dot to the spiral.']] },
      { kind: 'tap', dur: 6, beats: [[0, 'Tap the sundial for your days.'], [.4, 'Each day is a shell, with its date, its title and the times you spoke.']] },
      { kind: 'hold', dur: 8, beats: [[0, 'Hold it, and see where your days have come to rest.'], [.32, 'Tap a colour to hear what rests there.'], [.52, 'Hold the shell to your ear, and your own words come back.'], [.86, 'Then one question, back to today.']] }] },
    chat: { stop: 2, poster: .7, scenes: [{ kind: 'chat', dur: 6, beats: [[0, 'Or talk with your own Claude, wherever you are.'], [.5, 'The same days, kept in the same place.']] }] },
    bring: { stop: 3, poster: .8, scenes: [{ kind: 'bring', dur: 7, beats: [[0, 'Tap bring someone in, and send a friend your link.'], [.5, 'When they keep their first day, a thread runs from you to them.']] }] },
    bay: { stop: 4, poster: .75, scenes: [{ kind: 'bay', dur: 7, beats: [[0, 'Everyone who kept a day today, like starlings over the water.'], [.45, 'And the threads of who brought whom, as the community grows.']] }] },
    back: { stop: 5, poster: .8, scenes: [{ kind: 'back', dur: 6, beats: [[0, 'Every ninth day, the spiral turns.'], [.5, 'Look back on nine days, then eighty-one, as a page of their own.']] }] },
    night: { stop: 6, poster: .4, scenes: [{ kind: 'night', dur: 6, beats: [[0, 'Later, a night watch.'], [.45, 'A quieter place, for telling the longer story.']] }] }
  };
  var ORDER = ['what', 'listen', 'day', 'sundial', 'chat', 'bring', 'bay', 'back', 'night'];

  // Each beat is given time to read its line and watch its gesture: at least
  // 1.6s plus a third of a second a word, and never faster than 1.4 times the
  // gesture's own pace. The gesture inside a beat is stretched to fit, so the
  // words and the hands stay together.
  ORDER.forEach(function (name) {
    var f = FILMS[name];
    f.scenes.forEach(function (sc) {
      sc.secs = sc.beats.map(function (b, j) {
        var span = (j + 1 < sc.beats.length ? sc.beats[j + 1][0] : 1) - b[0];
        return Math.max(1.6 + b[1].split(' ').length / 3, span * sc.dur * 1.4);
      });
      sc.real = sc.secs.reduce(function (a, x) { return a + x; }, 0);
    });
    f.length = f.scenes.reduce(function (a, s) { return a + s.real; }, 0);
  });

  var C = { x: 232, y: 140 };
  // P6 order round presence, clockwise from Facts at the top.
  var P6 = [['#4A6B8A', 'what happened'], ['#7FB2E5', 'what’s ahead'], ['#7A5C99', 'what you made of it'], ['#B8860B', 'a choice'], ['#2E7D6B', 'what matters'], ['#B0413E', 'what you felt']];
  var SHARE = [.24, .2, .12, .08, .1, .26];
  var FLOW = 'M 375,25 C 320,45 262,78 215,102 C 196,113 186,138 192,156 C 199,172 232,180 262,174 C 300,166 304,122 284,106 C 264,92 240,96 222,108 C 195,118 172,165 150,212 C 122,262 68,300 14,324';
  var NS = 'http://www.w3.org/2000/svg';
  function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }
  function seg(u, a, b) { return clamp((u - a) / (b - a), 0, 1); }
  function ease(x) { return x < .5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2; }
  function lerp(a, b, k) { return a + (b - a) * k; }
  function mix(c1, c2, k) {
    var a = parseInt(c1.slice(1), 16), b = parseInt(c2.slice(1), 16);
    var r = Math.round(lerp(a >> 16, b >> 16, k)), g = Math.round(lerp((a >> 8) & 255, (b >> 8) & 255, k)), bl = Math.round(lerp(a & 255, b & 255, k));
    return '#' + ((1 << 24) + (r << 16) + (g << 8) + bl).toString(16).slice(1);
  }
  var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var mounted = [], count = 0;

  function mount(root, name) {
    var film = FILMS[name]; if (!film) return null;
    var P = 'sf' + (++count) + '-';
    var art = document.getElementById('sundialFilmArt').innerHTML
      .replace(/id="/g, 'id="' + P).replace(/url\(#/g, 'url(#' + P).replace(/href="#/g, 'href="#' + P);
    var title = film.stop == null ? film.title : ROAD[film.stop].name;
    var state = film.stop == null ? '' : ROAD[film.stop].state;
    root.innerHTML =
      '<div class="stage" tabindex="0" role="button" aria-label="Play the film: ' + title + '">' + art + '<div class="playmark"><span></span></div></div>' +
      '<div class="cap"><div class="name"></div><div class="line"></div><div class="state"></div></div>' +
      '<div class="ctrl"><button type="button" class="pp">play</button><button type="button" class="sh">share</button>' +
      '<a class="cta" href="' + HOME + '">Keep your first day ›</a></div><div class="toast" aria-live="polite"></div>';
    function $(id) { return document.getElementById(P + id); }
    function q(c) { return root.querySelector(c); }
    function el(tag, at, parent, text) { var e = document.createElementNS(NS, tag); for (var k in at) e.setAttribute(k, at[k]); if (text != null) e.textContent = text; if (parent) parent.appendChild(e); return e; }
    function set(id, at) { var e = typeof id === 'string' ? $(id) : id; for (var k in at) e.setAttribute(k, at[k]); }

    // The colour inside the ink: six stretches of the flow, in P6 order.
    P6.forEach(function (c, k) {
      el('path', { d: FLOW, fill: 'none', stroke: c[0], 'stroke-width': 6, pathLength: 1000, 'stroke-dasharray': '150 850', 'stroke-dashoffset': String(-(10 + k * 160)), opacity: .8 }, $('colour'));
    });
    // The spiral of days.
    function dayPos(n) { var a = (-90 + 40 * (n - 1)) * Math.PI / 180, r = 184 + 2.2 * (n - 1); return { x: C.x + r * Math.cos(a), y: C.y + r * Math.sin(a) }; }
    function spiralTo(n) {
      if (n <= 0) return '';
      var d = '', steps = Math.max(2, Math.round((n - 1) * 8));
      for (var i = 0; i <= steps; i++) { var p = dayPos(1 + (n - 1) * i / steps); d += (i ? ' L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1); }
      return d;
    }
    var dayDots = [];
    for (var i = 1; i <= 11; i++) dayDots.push(el('circle', { r: 6, fill: 'var(--ink)', opacity: 0 }, $('days')));
    var fold = el('circle', { r: 5.5, fill: 'none', stroke: 'var(--ink)', 'stroke-width': 1.5, opacity: 0 }, $('days'));

    // The shapeless conversation: bubbles stacking and curling inward.
    var loopBubbles = [];
    for (var j = 0; j < 16; j++) {
      var a = j * .62, r = 190 - j * 10.5, x = C.x + r * Math.cos(a) * 1.05, y = C.y + r * Math.sin(a) * .8, w = 120 - j * 4.5;
      loopBubbles.push(el('rect', { x: (x - w / 2).toFixed(1), y: (y - 11).toFixed(1), width: w.toFixed(1), height: 22, rx: 11, fill: j % 2 ? 'var(--ink)' : 'var(--page)', 'fill-opacity': j % 2 ? .7 : 1, stroke: 'var(--frame)', opacity: 0 }, $('loop')));
    }

    // A sundial shell: a flat coil with soft bands for the places a day touched.
    function coil(parent, x, y, R, places, w) {
      var g = el('g', { transform: 'translate(' + x + ' ' + y + ')' }, parent), pts = [], turns = 1.8, n = 90;
      el('circle', { r: R + w * .6, fill: 'var(--page)', stroke: 'var(--frame)', 'stroke-width': .8 }, g);
      for (var i = 0; i <= n; i++) { var t = i / n, a = t * turns * 6.283 - 1.57, r = R * (.12 + .88 * t); pts.push([r * Math.cos(a), r * Math.sin(a)]); }
      function part(a, b) { var d = ''; for (var i = Math.floor(a * n); i <= Math.ceil(b * n); i++) d += (d ? ' L' : 'M') + pts[i][0].toFixed(1) + ' ' + pts[i][1].toFixed(1); return d; }
      var k = places.length;
      places.forEach(function (p, j) { el('path', { d: part(j / k + .01, (j + 1) / k - .01), fill: 'none', stroke: P6[p][0], 'stroke-width': w, 'stroke-linecap': 'round', opacity: .62 }, g); });
      el('path', { d: part(0, 1), fill: 'none', stroke: 'var(--ink)', 'stroke-width': 1.1 }, g);
      el('circle', { r: 1.6, fill: 'var(--crimson)' }, g);
      return g;
    }
    // Tap: the strandline, newest first.
    var rows = [['FRIDAY 23 OCTOBER', 'Lizards and the first quiet', '07:05', [0, 1, 5]],
     ['THURSDAY 22 OCTOBER', 'Before the water park', '05:12 · 06:40', [0, 5, 1]],
     ['WEDNESDAY 21 OCTOBER', 'The old town in the heat', '18:20', [2, 4]]].map(function (r, i) {
      var y = 202 + i * 52, row = el('g', { opacity: 0 }, $('strand'));
      coil(row, 76, y + 10, 18, r[3], 4);
      el('text', { x: 108, y: y, 'font-size': 10.5, 'letter-spacing': 2.2, fill: 'var(--muted)' }, row, r[0]);
      el('text', { x: 108, y: y + 20, 'font-size': 19, 'font-style': 'italic', fill: 'var(--ink)' }, row, r[1]);
      el('text', { x: 108, y: y + 35, 'font-size': 12, fill: 'var(--muted)' }, row, r[2]);
      return row;
    });
    el('line', { x1: 76, y1: 196, x2: 76, y2: 350, stroke: 'var(--ink)', 'stroke-opacity': .18 }, $('strand'));
    // Hold: six pools round the sundial, sized by share, in P6 order.
    var poolEls = P6.map(function (p, k) {
      var a = (-90 + 60 * k) * Math.PI / 180, x = 232 + 150 * Math.cos(a), y = 92 + 118 * Math.sin(a), r = 9 + 34 * SHARE[k];
      var g = el('g', { opacity: 0 }, $('pools'));
      el('circle', { cx: x, cy: y, r: r, fill: p[0], 'fill-opacity': .55, stroke: p[0] }, g);
      return { g: g, x: x, y: y };
    });
    el('text', { x: 232, y: 250, 'font-size': 15, 'font-style': 'italic', 'text-anchor': 'middle', fill: 'var(--muted)' }, $('pools'), 'where your days have come to rest');
    // The shell that listens, and words rising from it.
    (function () {
      var d = '', n = 120, big = $('shellBig');
      for (var i = 0; i <= n; i++) { var t = i / n, a = t * 2.2 * 6.283, r = 40 * Math.pow(t, 1.3); d += (i ? ' L' : 'M') + (r * Math.cos(a)).toFixed(1) + ' ' + (r * Math.sin(a)).toFixed(1); }
      el('circle', { r: 41, fill: '#B0413E', 'fill-opacity': .32, stroke: 'var(--ink)', 'stroke-width': 2 }, big);
      el('path', { d: d, fill: 'none', stroke: 'var(--ink)', 'stroke-width': 1.3, 'stroke-opacity': .75 }, big);
    })();
    var words = ['the day only just beginning', 'your ankles are sore', 'hoping for a sunny day'].map(function (w) {
      return el('text', { 'font-size': 16, 'font-style': 'italic', 'text-anchor': 'middle', fill: 'var(--ink)', opacity: 0 }, $('rising'), w);
    });
    // How it listens: the person's own words, and the places they touch.
    var LW = [['Woke early. The house was quiet.', 0, 'what happened'], ['I felt flat, then lighter by lunch.', 5, 'what you felt'], ['Tomorrow I see my sister.', 1, 'what’s ahead']];
    var lLines = LW.map(function (w, i) {
      var y = 190 + i * 34, g = el('g', {}, $('lWords'));
      el('text', { x: 62, y: y, 'font-size': 17, fill: 'var(--ink)' }, g, w[0]);
      var ul = el('line', { x1: 62, y1: y + 7, x2: 62, y2: y + 7, stroke: P6[w[1]][0], 'stroke-width': 3, 'stroke-linecap': 'round', opacity: .75 }, g);
      var lab = el('text', { x: 408, y: y, 'font-size': 12.5, 'font-style': 'italic', 'text-anchor': 'end', fill: P6[w[1]][0], opacity: 0 }, g, w[2]);
      return { g: g, ul: ul, lab: lab, w: [226, 236, 176][i] };
    });
    // A small sundial for a friend and for the bay.
    function mini(parent, x, y, s) {
      var g = el('g', { transform: 'translate(' + x + ' ' + y + ') scale(' + s + ') translate(' + (-C.x) + ' ' + (-C.y) + ')' }, parent);
      el('ellipse', { cx: C.x, cy: C.y, rx: 130, ry: 124, fill: '#F1E79B', opacity: .55 }, g);
      el('use', { href: '#' + P + 'heartInk', fill: 'var(--ink)' }, g);
      el('ellipse', { cx: 250, cy: 138, rx: 40, ry: 24, transform: 'rotate(-18 250 138)', fill: 'var(--crimson)' }, g);
      return g;
    }
    var friendPos = { x: 372, y: 196 };
    mini($('friendDial'), friendPos.x, friendPos.y, .3);
    var friendDot = el('circle', { cx: friendPos.x, cy: friendPos.y - 58, r: 3.2, fill: 'var(--ink)', opacity: 0 }, $('friendDial'));
    var shoreAt = [[44, 286, .15], [102, 290, .17], [158, 300, .16], [214, 298, .19], [270, 290, .16], [326, 287, .18], [382, 296, .15], [432, 287, .14]];
    var bayMinis = shoreAt.map(function (p) { var g = mini($('bayMinis'), p[0], p[1] - 22, p[2]); g.setAttribute('opacity', 0); return g; });
    var bayThreads = [[3, 1], [3, 5], [1, 0], [5, 6], [1, 2], [5, 4], [6, 7]].map(function (b) {
      var a = shoreAt[b[0]], c = shoreAt[b[1]];
      return el('path', { d: 'M' + a[0] + ' ' + (a[1] - 46) + ' Q' + (a[0] + c[0]) / 2 + ' ' + (Math.min(a[1], c[1]) - 70) + ' ' + c[0] + ' ' + (c[1] - 46), fill: 'none', stroke: 'var(--crimson)', 'stroke-width': 1.1, 'stroke-opacity': .7, pathLength: 100, opacity: 0 }, $('bayThreads'));
    });
    var birds = [];
    for (var b = 0; b < 72; b++) birds.push({ e: el('path', { d: 'M-2.4 0 L0 1.4 L2.4 0', fill: 'none', stroke: 'var(--ink)', 'stroke-width': 1.1, 'stroke-linecap': 'round', opacity: 0 }, $('birds')),
      p: Math.random() * 6.283, q: Math.random() * 6.283, r: .35 + Math.random() * .65, k: Math.random() });

    // The caption: the film's name (and, on the road, its state), and a line that moves with the hands.
    var shown = '', instant = false;
    q('.cap .name').textContent = title;
    if (state) { var st = q('.cap .state'); st.textContent = state === 'here' ? 'here now' : state; st.className = 'state ' + state.replace(' ', '-'); }
    function caption(line, key) {
      if (key === shown) return;
      shown = key;
      var ln = q('.cap .line');
      if (instant) { ln.textContent = line; ln.style.opacity = 1; return; }
      ln.style.opacity = 0;
      setTimeout(function () { ln.textContent = line; ln.style.opacity = 1; }, 320);
    }

    function finger(x, y, show, tapAt, u) {
      set('finger', { transform: 'translate(' + x.toFixed(1) + ' ' + y.toFixed(1) + ')', opacity: show });
      var r = tapAt == null ? 0 : seg(u, tapAt, tapAt + .1);
      set('ripple', { r: (12 + 18 * r).toFixed(1), opacity: r > 0 && r < 1 ? (1 - r) * .7 : 0 });
    }
    function glide(u, a, b, from, to) { var k = ease(seg(u, a, b)); return [lerp(from[0], to[0], k), lerp(from[1], to[1], k)]; }

    function render(t) {
      var k = 0, start = 0, scenes = film.scenes;
      while (k < scenes.length - 1 && t >= start + scenes[k].real) { start += scenes[k].real; k++; }
      var sc = scenes[k], local = t - start, bi = 0, acc = 0;
      while (bi < sc.secs.length - 1 && local >= acc + sc.secs[bi]) { acc += sc.secs[bi]; bi++; }
      var b0 = sc.beats[bi][0], b1 = bi + 1 < sc.beats.length ? sc.beats[bi + 1][0] : 1;
      var u = clamp(b0 + (b1 - b0) * (local - acc) / sc.secs[bi], 0, 1);
      caption(sc.beats[bi][1], k + ':' + bi);
      var V = {
        dialS: .62, dialX: 232, dialY: 64, dialO: 1, sun: 1, hour: 10.5, colour: .8, days: 8, night: 0, glow: 0, foldK: 0, draw: 1, crim: 1,
        loop: 0, listen: 0, name: 0,
        speak: 0, sheet: 0, btns: 1, kept: 0, strand: 0, pools: 0, shell: 0, chat: 0, bring: 0, invite: 0, friend: 0, bay: 0, hold: 0, fing: 0, fx: 300, fy: 340, tap: null
      };
      if (sc.kind === 'what') {
        // 0 the heart draws itself; 1 the shapeless conversation curls in; 2 it clears and the light comes;
        // 3 a day in, a sheet back; 4 following; 5 the instructions, honestly; 6 the sun crosses, the day rests;
        // 7 a dot under your name; 8 days gather; 9 the made-by, in full light.
        var lift2 = ease(seg(u, .29, .31)) * (1 - ease(seg(u, .59, .62)));
        V.dialS = lerp(.62, .46, lift2); V.dialY = lerp(92, 34, lift2); V.days = 0; V.colour = 0;
        V.draw = ease(seg(u, .01, .09)); V.crim = seg(u, .06, .1);
        V.sun = seg(u, .2, .27) * (1 - seg(u, .65, .7) * .45) + seg(u, .8, .86) * .45; V.hour = lerp(7, 11, seg(u, .2, .3));
        V.loop = seg(u, .1, .12) * (1 - seg(u, .19, .22)); V.dialO = 1 - .65 * seg(u, .1, .14) * (1 - seg(u, .19, .23));
        V.speak = seg(u, .3, .31) * (1 - seg(u, .345, .355));
        var said = 'A slow morning by the water…';
        $('said').textContent = said.slice(0, Math.round(said.length * seg(u, .305, .34)));
        V.sheet = seg(u, .355, .365) * (1 - seg(u, .58, .6)); V.btns = 0;
        V.glow = seg(u, .41, .45) * (1 - seg(u, .48, .5)) * .6;
        if (u > .6) { V.hour = lerp(11, 19, ease(seg(u, .6, .69))); }
        if (u > .69) { V.days = 1; V.name = seg(u, .71, .74) * (1 - seg(u, .79, .8)); }
        if (u > .8) { V.days = 1 + Math.floor(seg(u, .8, .88) * 6.99); V.colour = .3 + .5 * seg(u, .8, .88); V.hour = lerp(19, 12, ease(seg(u, .8, .86))); }
        if (u > .9) { V.days = 8; V.colour = .8; }
      }
      if (sc.kind === 'listen') {
        // 0 held; 1 noticed; 2 followed; 3 reflected; 4 returned to now; 5 room to live.
        V.dialS = .3; V.dialY = 30; V.days = 3; V.colour = .3; V.hour = 9; V.sun = .7;
        V.listen = seg(u, 0, .02) * (1 - seg(u, .79, .83));
        if (u > .66) { V.hour = lerp(9, 12, seg(u, .66, .76)); V.sun = 1; }
        if (u > .83) { V.dialS = lerp(.3, .78, ease(seg(u, .83, .9))); V.dialY = lerp(30, 120, ease(seg(u, .83, .9))); V.colour = .6; V.days = 4; }
      }
      if (sc.kind === 'speak') {
        V.days = u > .84 ? 1 : 0; V.colour = u > .84 ? seg(u, .84, 1) * .5 : 0; V.sun = .6; V.hour = 9;
        V.speak = seg(u, 0, .06) * (1 - seg(u, .42, .48));
        var said2 = 'A slow morning by the water. The kids laughing…';
        $('said').textContent = said2.slice(0, Math.round(said2.length * seg(u, .16, .4)));
        V.sheet = seg(u, .46, .52) * (1 - seg(u, .78, .82));
        V.kept = seg(u, .82, .86) * (1 - seg(u, .97, 1));
        var f = u < .5 ? glide(u, .02, .12, [300, 380], [104, 325]) : glide(u, .55, .66, [104, 325], [138, 329]);
        V.fx = f[0]; V.fy = f[1]; V.fing = seg(u, .02, .08) * (1 - seg(u, .36, .4)) + seg(u, .55, .6) * (1 - seg(u, .78, .82)); V.tap = u < .5 ? .13 : .7;
      }
      if (sc.kind === 'light') { V.dialS = lerp(.62, .9, ease(seg(u, 0, .15))); V.dialY = lerp(64, 128, ease(seg(u, 0, .15))); V.hour = 6.3 + 13 * u; V.days = 1 + Math.floor(u * 5.99); V.colour = .5 + .4 * u; }
      if (sc.kind === 'tap') {
        V.days = 6; V.hour = 14;
        var up = ease(seg(u, .25, .4)); V.dialS = lerp(.9, .36, up); V.dialY = lerp(128, 52, up);
        var f2 = glide(u, 0, .18, [380, 360], [232, 140]); V.fx = f2[0]; V.fy = f2[1]; V.fing = seg(u, 0, .06) * (1 - seg(u, .3, .36)); V.tap = .2;
        V.strand = seg(u, .4, .5);
      }
      if (sc.kind === 'hold') {
        V.days = 6; V.hour = 15; V.dialS = .36; V.dialY = 92;
        V.fx = 232; V.fy = 96; V.fing = seg(u, 0, .05) * (1 - seg(u, .3, .34)); V.hold = seg(u, .05, .25);
        V.pools = seg(u, .25, .32) * (1 - seg(u, .5, .55));
        if (u > .38 && u < .5) { V.fing = seg(u, .38, .41); V.fx = poolEls[5].x; V.fy = poolEls[5].y; V.tap = .43; }
        V.shell = seg(u, .52, .58) * (1 - seg(u, .96, 1)); V.dialO = 1 - seg(u, .5, .55);
      }
      if (sc.kind === 'chat') { V.dialS = .5; V.dialY = 40; V.hour = 19; V.days = 7 + (u > .8 ? 1 : 0); V.chat = seg(u, 0, .06) * (1 - seg(u, .94, 1)); }
      if (sc.kind === 'bring') {
        V.days = 8; V.hour = 12; var m = ease(seg(u, 0, .15)); V.dialS = lerp(.5, .55, m); V.dialX = lerp(232, 150, m); V.dialY = lerp(40, 100, m);
        V.bring = seg(u, .1, .18) * (1 - seg(u, .5, .55)); V.invite = seg(u, .3, .36) * (1 - seg(u, .5, .55));
        V.fx = 186; V.fy = 241; V.fing = seg(u, .15, .2) * (1 - seg(u, .32, .36)); V.tap = .24;
        V.friend = seg(u, .5, .58) * (1 - seg(u, .95, 1));
      }
      if (sc.kind === 'bay') { V.dialO = 0; V.bay = seg(u, 0, .12) * (1 - seg(u, .93, 1)); }
      if (sc.kind === 'back') { V.dialS = .85; V.dialY = 128; V.dialO = seg(u, 0, .15); V.hour = 16 + u * 2; V.days = 8 + (u > .25 ? 1 : 0); V.foldK = ease(seg(u, .35, .65)); if (u > .72) V.days = 10; if (u > .86) V.days = 11; }
      if (sc.kind === 'night') { V.dialS = .85; V.dialY = 128; V.night = ease(seg(u, .05, .35)) * (1 - ease(seg(u, .88, 1))); V.hour = 19 + u * 3; V.days = 11; V.foldK = 1; V.glow = V.night; }

      // The sun and the heart's shadow.
      var s = clamp(Math.sin(Math.PI * (V.hour - 6) / 13.5), 0, 1);
      var sha = (-90 + (V.hour - 12) * 15) * Math.PI / 180, L = 10 + 40 * (1 - s);
      var sunCol = V.hour < 10 ? mix('#F3CB8F', '#F1E79B', seg(V.hour, 6, 10)) : V.hour > 15 ? mix('#F1E79B', '#EDB98A', seg(V.hour, 15, 19)) : '#F1E79B';
      set('sun0', { 'stop-color': sunCol }); set('sun1', { 'stop-color': sunCol });
      var toward = 36 * (1 - s) + 10;
      set('sun', { cx: C.x - Math.cos(sha) * toward, cy: C.y - Math.sin(sha) * toward });
      set('sunWash', { opacity: V.sun * (1 - V.night) * (s > 0 ? .35 + .6 * s : 0) });
      set('shadow', { opacity: V.sun * V.draw * (1 - V.night) * .16 * Math.min(1, s * 4), transform: 'translate(' + (Math.cos(sha) * L).toFixed(1) + ' ' + (Math.sin(sha) * L).toFixed(1) + ')' });
      set('colour', { opacity: V.colour * (1 - V.night * .5) });
      set('centreGlow', { opacity: V.glow });
      set('night', { opacity: V.night * .9 });
      set('star', { opacity: V.night * (.6 + .4 * Math.sin(t * 3)) });
      set('mRing', { 'stroke-dashoffset': (1000 * (1 - V.draw)).toFixed(0) });
      set('mFlow', { 'stroke-dashoffset': (1000 * (1 - V.draw)).toFixed(0) });
      set('crim', { opacity: V.crim });
      var inkCol = mix('#1d1a17', '#efe6d2', V.night);
      set('heart', { fill: inkCol });
      set('spiral', { stroke: inkCol, d: spiralTo(V.days) });
      dayDots.forEach(function (d, i) {
        var n = i + 1, p = dayPos(n), on = n <= V.days ? 1 : 0, r = 5.5;
        if (V.foldK > 0 && n < 9) { r = lerp(5.5, 2.2, V.foldK); on = on ? lerp(1, .4, V.foldK) : 0; }
        if (n === 9) r = 0;
        set(d, { cx: p.x.toFixed(1), cy: p.y.toFixed(1), opacity: on, r: r.toFixed(2), fill: inkCol });
      });
      var nine = dayPos(9), one = dayPos(1);
      set(fold, { cx: nine.x.toFixed(1), cy: nine.y.toFixed(1), opacity: V.days >= 9 ? 1 : 0, stroke: inkCol });
      set('nameTag', { x: (one.x + 18).toFixed(1), y: (one.y + 10).toFixed(1), opacity: V.name });
      set('holdRing', { 'stroke-dasharray': (V.hold * 100).toFixed(1) + ' 100', opacity: V.hold > 0 && V.pools < 1 ? .8 : 0 });
      set('dial', { transform: 'translate(' + V.dialX + ' ' + V.dialY + ') scale(' + V.dialS + ') translate(' + (-C.x) + ' ' + (-C.y) + ')', opacity: V.dialO });

      // The shapeless conversation, stacking and curling in, each one dimmer.
      set('loop', { opacity: V.loop });
      loopBubbles.forEach(function (bb, i) { bb.setAttribute('opacity', (seg(u, .11 + i * .004, .114 + i * .004) * (1 - i * .045)).toFixed(2)); });
      // Speaking, the sheet, keeping.
      set('speak', { opacity: V.speak }); set('sheet', { opacity: V.sheet }); set('sheetBtns', { opacity: V.btns }); set('kept', { opacity: V.kept });
      // How it listens.
      set('listen', { opacity: V.listen });
      if (sc.kind === 'listen') {
        set('holdFrame', { opacity: seg(u, .02, .1) });
        lLines.forEach(function (l, i) {
          var nk = seg(u, 1 / 6 + i * .04, 1 / 6 + .05 + i * .04);
          l.ul.setAttribute('x2', (62 + l.w * nk).toFixed(1)); l.lab.setAttribute('opacity', nk);
          var fade = i === 1 ? 1 : 1 - .55 * seg(u, 2 / 6, 2 / 6 + .04);
          l.g.setAttribute('opacity', (fade * (1 - seg(u, 4 / 6, 4 / 6 + .04) * (i === 1 ? .5 : 1))).toFixed(2));
        });
        var tk = seg(u, 2 / 6 + .03, 2 / 6 + .12);
        set('lThread', { d: 'M 236 232 C 300 250 250 284 232 298', 'stroke-dasharray': (tk * 100).toFixed(1) + ' 100', opacity: 1 - seg(u, 4 / 6, 4 / 6 + .04) });
        set('lReply', { opacity: seg(u, .5, .54) * (1 - seg(u, 4 / 6, 4 / 6 + .03)) });
        set('lNow', { opacity: seg(u, 4 / 6 + .04, 4 / 6 + .08) });
      }
      // Tap: the strandline.
      set('strand', { opacity: V.strand });
      rows.forEach(function (r, i) { r.setAttribute('opacity', seg(u, .42 + i * .1, .5 + i * .1)); });
      // Hold: the pools, then the shell to the ear.
      set('pools', { opacity: V.pools });
      poolEls.forEach(function (p, i) { p.g.setAttribute('opacity', seg(u, .25 + i * .015, .3 + i * .015)); });
      set('shellScene', { opacity: V.shell });
      var lift = ease(seg(u, .6, .7));
      set('shellBig', { transform: 'translate(' + lerp(232, 352, lift) + ' ' + lerp(280, 262, lift) + ') rotate(' + (-25 * lift) + ') scale(' + lerp(1, .9, lift) + ')' });
      words.forEach(function (w, i) {
        var r = seg(u, .68 + i * .06, .8 + i * .06), x = lerp(352, 150, r), y = lerp(262, 236 + i * 26, r) - Math.sin(r * Math.PI) * 20;
        set(w, { x: x.toFixed(1), y: y.toFixed(1), opacity: r > 0 ? Math.min(1, r * 2) : 0 });
      });
      set('shellQ', { opacity: seg(u, .88, .94) });
      // In a chat.
      set('chat', { opacity: V.chat });
      set('b0', { width: 210 * seg(u, .06, .16) }); set('t0', { opacity: seg(u, .14, .2) });
      var w1 = 230 * seg(u, .3, .4); set('b1', { width: w1, x: 408 - w1 }); set('t1', { opacity: seg(u, .38, .44) });
      set('b2', { width: 228 * seg(u, .55, .65) }); set('t2', { opacity: seg(u, .63, .69) });
      // Bring someone in.
      set('bring', { opacity: V.bring }); set('invite', { opacity: V.invite });
      set('friend', { opacity: V.friend });
      var fx = V.dialX + 126 * V.dialS, fy = V.dialY - 30 * V.dialS, tx = friendPos.x, ty = friendPos.y - 58, prog = seg(u, .58, .85);
      set('thread', { d: 'M' + fx.toFixed(1) + ' ' + fy.toFixed(1) + ' Q ' + ((fx + tx) / 2).toFixed(1) + ' ' + (Math.min(fy, ty) - 60).toFixed(1) + ' ' + tx + ' ' + ty, pathLength: 100, 'stroke-dasharray': (prog * 100).toFixed(1) + ' 100' });
      set(friendDot, { opacity: seg(u, .85, .9) });
      // The bay.
      set('bay', { opacity: V.bay });
      if (sc.kind === 'bay') {
        bayMinis.forEach(function (g, i) { g.setAttribute('opacity', seg(u, .12 + i * .05, .2 + i * .05)); });
        bayThreads.forEach(function (p, i) { var a = seg(u, .35 + i * .06, .45 + i * .06); p.setAttribute('opacity', a > 0 ? .9 : 0); p.setAttribute('stroke-dasharray', (a * 100).toFixed(1) + ' 100'); });
        var cx = 232 + 70 * Math.sin(t * .37), cy = 70 + 26 * Math.sin(t * .53 + 1), spread = 52 + 22 * Math.sin(t * .29);
        birds.forEach(function (bd) {
          var a = bd.p + t * .6 + Math.sin(t * .7 + bd.q) * .8, rr = spread * bd.r * (1 + .25 * Math.sin(t * 1.3 + bd.q));
          bd.e.setAttribute('transform', 'translate(' + (cx + Math.cos(a) * rr * 1.7).toFixed(1) + ' ' + (cy + Math.sin(a) * rr * .55 + Math.sin(t * 2 + bd.k * 9) * 3).toFixed(1) + ') rotate(' + (Math.cos(a) * 20).toFixed(0) + ')');
          bd.e.setAttribute('opacity', seg(u, .2 + bd.k * .3, .3 + bd.k * .3));
        });
      }
      finger(V.fx, V.fy, V.fing, V.tap, u);
    }

    // Playing: a film rests on a still until tapped, plays once, and rests on its
    // last line with 'again' and the way in. Only one film plays at a time.
    var pos = 0, last = null, playing = false, beat = -1;
    var beats = []; // the start of every beat, for stepping when motion is reduced
    (function () { var at = 0; film.scenes.forEach(function (sc) { sc.secs.forEach(function (s) { beats.push(at + s * .6); at += s; }); }); })();
    function ui() {
      root.classList.toggle('playing', playing);
      q('.pp').textContent = playing ? 'pause' : (pos >= film.length - .05 ? 'again' : (pos > 0 ? 'play' : 'play'));
    }
    function frame(ts) {
      if (!playing) { last = null; return; }
      if (last === null) last = ts;
      pos += Math.min(.1, (ts - last) / 1000); last = ts;
      if (pos >= film.length) { pos = film.length - .01; playing = false; root.classList.add('ended'); rest(lastLine()); ui(); return; }
      render(pos);
      requestAnimationFrame(frame);
    }
    var api = {
      name: name,
      play: function () {
        mounted.forEach(function (m) { if (m !== api) m.pause(); });
        if (reduce) { beat = (beat + 1) % beats.length; render(beats[beat]); root.classList.toggle('ended', beat === beats.length - 1); q('.pp').textContent = beat === beats.length - 1 ? 'again' : 'next line'; return; }
        if (pos >= film.length - .05 || pos === 0) { pos = 0; }
        root.classList.remove('ended'); playing = true; ui(); requestAnimationFrame(frame);
      },
      pause: function () { if (playing) { playing = false; ui(); } },
      toggle: function () { playing ? api.pause() : api.play(); },
      seek: function (t) { pos = clamp(t, 0, film.length - .01); render(pos); },
      length: film.length,
      root: root
    };
    function share() {
      var from = window.SUNDIAL_FROM ? '&from=' + encodeURIComponent(window.SUNDIAL_FROM) : '';
      var url = FILM_BASE + name + '.html' + (from ? '?' + from.slice(1) : ''), label = 'Sundial: ' + title;
      if (navigator.share) { navigator.share({ title: label, url: url }).catch(function () {}); return; }
      var done = function () { q('.toast').textContent = 'Link copied: ' + url.replace(/^https?:\/\//, ''); };
      try { navigator.clipboard.writeText(url).then(done, function () { q('.toast').textContent = url; }); } catch (e) { q('.toast').textContent = url; }
    }
    q('.stage').addEventListener('click', api.toggle);
    q('.stage').addEventListener('keydown', function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); api.toggle(); } });
    q('.pp').addEventListener('click', api.toggle);
    q('.sh').addEventListener('click', share);
    // The still it rests on, before it is played and after: a moment that says what the film is.
    function lastLine() { var sc = film.scenes[film.scenes.length - 1]; return sc.beats[sc.beats.length - 1][1]; }
    function rest(line) { instant = true; render(film.length * film.poster); shown = ''; q('.cap .line').textContent = line; instant = false; }
    rest((film.stop == null ? '' : ROAD[film.stop].line) || film.scenes[0].beats[0][1]); pos = 0;
    if (reduce) q('.pp').textContent = 'first line';
    mounted.push(api);
    return api;
  }

  // Mount every film on the page; a page opened with ?film=name plays that one.
  [].forEach.call(document.querySelectorAll('[data-film]'), function (el) { mount(el, el.getAttribute('data-film')); });
  var want = (location.search.match(/[?&]film=([a-z]+)/) || [])[1];
  if (want) mounted.forEach(function (m) { if (m.name === want) { m.root.scrollIntoView({ block: 'center' }); } });

  window.SundialFilms = { mount: mount, FILMS: FILMS, ORDER: ORDER, mounted: mounted };
})();
