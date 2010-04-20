# PR#234, insertion order

a1 = {'status'      :1,
      'background'  :2,
      'borderwidth' :3,
      'foreground'  :4,
      }

a2 = {'status'      :1,
      'background'  :2,
      'foreground'  :4,
      'borderwidth' :3,
      }

if a1 <> a2:
    print 'dictionaries did not compare equal!'
